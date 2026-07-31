package one.srz.jellywear.presentation.login

import android.app.RemoteInput
import android.content.Intent
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.presentation.library.errorMessageRes
import org.jellyfin.sdk.api.client.ApiClient

private const val REMOTE_INPUT_KEY = "text"
private const val QUICK_CONNECT_POLL_INTERVAL_MS = 3000L
private const val QUICK_CONNECT_MAX_ATTEMPTS = 60 // ~3 minutes

private enum class LoginStep {
    SERVER, CONNECTING, HTTP_WARNING, CHECKING, QUICK_CONNECT, USERNAME, PASSWORD, SIGNING_IN, ERROR
}

@Composable
fun LoginScreen(
    session: JellyfinSession,
    onLoggedIn: () -> Unit,
) {
    var step by remember { mutableStateOf(LoginStep.SERVER) }
    var client by remember { mutableStateOf<ApiClient?>(null) }
    var serverUrlHadExplicitScheme by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var quickConnectCode by remember { mutableStateOf<String?>(null) }
    var quickConnectSecret by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val serverLabel = stringResource(R.string.login_prompt_server)
    val usernameLabel = stringResource(R.string.login_prompt_username)
    val passwordLabel = stringResource(R.string.login_prompt_password)
    val genericError = stringResource(R.string.login_error_generic)
    val quickConnectTimeoutError = stringResource(R.string.login_quick_connect_timeout)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val rawText = result.data
            ?.let { RemoteInput.getResultsFromIntent(it) }
            ?.getCharSequence(REMOTE_INPUT_KEY)
            ?.toString()

        // No payload at all means the user backed out of the input UI.
        if (rawText == null) {
            errorMessage = genericError
            step = LoginStep.ERROR
            return@rememberLauncherForActivityResult
        }

        // Server URL and username can safely be trimmed, but a password must
        // be passed through untouched -- leading/trailing whitespace can be
        // part of a valid password. An empty password is legitimate too:
        // Jellyfin allows passwordless accounts.
        val text = if (step == LoginStep.PASSWORD) rawText else rawText.trim()
        if (text.isEmpty() && step != LoginStep.PASSWORD) {
            errorMessage = genericError
            step = LoginStep.ERROR
            return@rememberLauncherForActivityResult
        }

        when (step) {
            LoginStep.SERVER -> {
                serverUrlHadExplicitScheme = text.contains("://")
                step = LoginStep.CONNECTING
                scope.launch {
                    session.buildVerifiedClient(text).fold(
                        onSuccess = { verifiedClient ->
                            client = verifiedClient
                            // Whenever the verified connection ended up on
                            // cleartext http -- the https-first probe fell
                            // back, or the user typed http:// explicitly --
                            // interpose an explicit consent step before any
                            // credentials are asked for, let alone sent
                            // (see #23: usesCleartextTraffic stays enabled
                            // for LAN servers, so this opt-in is the guard).
                            step = if (verifiedClient.baseUrl?.startsWith("http://") == true) {
                                LoginStep.HTTP_WARNING
                            } else {
                                LoginStep.CHECKING
                            }
                        },
                        onFailure = { error ->
                            errorMessage = context.getString(errorMessageRes(error))
                            step = LoginStep.ERROR
                        },
                    )
                }
            }
            LoginStep.USERNAME -> {
                username = text
                step = LoginStep.PASSWORD
            }
            LoginStep.PASSWORD -> {
                val currentClient = client
                if (currentClient == null) {
                    errorMessage = genericError
                    step = LoginStep.ERROR
                } else {
                    step = LoginStep.SIGNING_IN
                    scope.launch {
                        var result = session.login(currentClient, username, text)

                        // The connectivity check in buildVerifiedClient is a
                        // GET, which some reverse proxies transparently
                        // redirect http -> https -- letting the check pass
                        // even though this login POST hits the same
                        // redirect and loses its body, failing where the
                        // GET didn't. One more try, upgraded to https,
                        // before giving up -- but only if the user didn't
                        // type a scheme explicitly (respect their choice).
                        // Never the reverse: a https -> http retry would
                        // resend the password in cleartext.
                        if (result.isFailure && !serverUrlHadExplicitScheme) {
                            val upgradedClient = currentClient.baseUrl
                                ?.let { session.buildClientWithUpgradedScheme(it) }
                            if (upgradedClient != null) {
                                val retryResult = session.login(upgradedClient, username, text)
                                if (retryResult.isSuccess) {
                                    client = upgradedClient
                                    result = retryResult
                                }
                            }
                        }

                        result.fold(
                            onSuccess = { onLoggedIn() },
                            onFailure = { error ->
                                errorMessage = context.getString(errorMessageRes(error))
                                step = LoginStep.ERROR
                            },
                        )
                    }
                }
            }
            LoginStep.CONNECTING, LoginStep.HTTP_WARNING, LoginStep.CHECKING,
            LoginStep.QUICK_CONNECT, LoginStep.SIGNING_IN, LoginStep.ERROR,
            -> Unit
        }
    }

    // Ask for the server URL, then decide between Quick Connect (no password
    // typing on the watch) and the RemoteInput username/password flow.
    LaunchedEffect(step) {
        when (step) {
            LoginStep.SERVER -> launcher.launch(buildRemoteInputIntent(serverLabel))
            LoginStep.USERNAME -> launcher.launch(buildRemoteInputIntent(usernameLabel))
            LoginStep.PASSWORD -> launcher.launch(buildRemoteInputIntent(passwordLabel))
            LoginStep.CHECKING -> {
                val currentClient = client
                if (currentClient == null) {
                    errorMessage = genericError
                    step = LoginStep.ERROR
                } else if (session.isQuickConnectEnabled(currentClient)) {
                    session.initiateQuickConnect(currentClient).fold(
                        onSuccess = { result ->
                            quickConnectCode = result.code
                            quickConnectSecret = result.secret
                            step = LoginStep.QUICK_CONNECT
                        },
                        onFailure = { step = LoginStep.USERNAME },
                    )
                } else {
                    step = LoginStep.USERNAME
                }
            }
            LoginStep.QUICK_CONNECT -> {
                val currentClient = client
                val secret = quickConnectSecret
                if (currentClient == null || secret == null) {
                    errorMessage = genericError
                    step = LoginStep.ERROR
                } else {
                    var attempts = 0
                    var loggedIn = false
                    while (!loggedIn && attempts < QUICK_CONNECT_MAX_ATTEMPTS) {
                        delay(QUICK_CONNECT_POLL_INTERVAL_MS)
                        val result = session.pollQuickConnect(currentClient, secret)
                        result.onFailure { error ->
                            errorMessage = context.getString(errorMessageRes(error))
                            step = LoginStep.ERROR
                        }
                        if (result.isFailure) return@LaunchedEffect
                        loggedIn = result.getOrDefault(false)
                        attempts++
                    }
                    if (loggedIn) {
                        onLoggedIn()
                    } else if (step == LoginStep.QUICK_CONNECT) {
                        errorMessage = quickConnectTimeoutError
                        step = LoginStep.ERROR
                    }
                }
            }
            LoginStep.CONNECTING, LoginStep.HTTP_WARNING, LoginStep.SIGNING_IN, LoginStep.ERROR -> Unit
        }
    }

    if (step == LoginStep.HTTP_WARNING) {
        HttpWarning(
            onContinue = { step = LoginStep.CHECKING },
            onCancel = {
                client = null
                step = LoginStep.SERVER
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (step) {
            LoginStep.CONNECTING, LoginStep.CHECKING, LoginStep.SIGNING_IN -> CircularProgressIndicator()
            LoginStep.QUICK_CONNECT -> {
                Text(
                    text = quickConnectCode ?: "",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.display2,
                )
                Text(
                    text = stringResource(R.string.login_quick_connect_hint),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            LoginStep.ERROR -> {
                Column(
                    modifier = Modifier.clickable { step = LoginStep.SERVER },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = errorMessage ?: genericError,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body2,
                    )
                    Text(
                        text = stringResource(R.string.login_retry_hint),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.caption2,
                    )
                }
            }
            LoginStep.SERVER, LoginStep.USERNAME, LoginStep.PASSWORD -> {
                Text(
                    text = stringResource(R.string.login_title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3,
                )
            }
            // Rendered by the early return above, never reaches this Column.
            LoginStep.HTTP_WARNING -> Unit
        }
    }
}

/**
 * Explicit consent step shown when the server connection ended up on
 * cleartext http (https-first probing fell back, or the user typed an
 * http:// URL). Canceling is the visually primary action; continuing is
 * the deliberate opt-in.
 */
@Composable
private fun HttpWarning(
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(
                    text = stringResource(R.string.http_warning_title),
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.http_warning_text),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        item {
            Chip(
                label = { Text(text = stringResource(R.string.http_warning_cancel)) },
                onClick = onCancel,
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
        item {
            Chip(
                label = { Text(text = stringResource(R.string.http_warning_continue)) },
                onClick = onContinue,
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

private fun buildRemoteInputIntent(label: String): Intent {
    val remoteInputs = listOf(
        RemoteInput.Builder(REMOTE_INPUT_KEY)
            .setLabel(label)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_DONE)
            }
            .build(),
    )

    return RemoteInputIntentHelper.createActionRemoteInputIntent().apply {
        RemoteInputIntentHelper.putRemoteInputsExtra(this, remoteInputs)
    }
}
