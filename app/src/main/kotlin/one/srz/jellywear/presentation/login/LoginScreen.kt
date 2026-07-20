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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession
import org.jellyfin.sdk.api.client.ApiClient

private const val REMOTE_INPUT_KEY = "text"
private const val QUICK_CONNECT_POLL_INTERVAL_MS = 3000L
private const val QUICK_CONNECT_MAX_ATTEMPTS = 60 // ~3 minutes

private enum class LoginStep {
    SERVER, CHECKING, QUICK_CONNECT, USERNAME, PASSWORD, CONNECTING, ERROR
}

@Composable
fun LoginScreen(
    session: JellyfinSession,
    onLoggedIn: () -> Unit,
) {
    var step by remember { mutableStateOf(LoginStep.SERVER) }
    var client by remember { mutableStateOf<ApiClient?>(null) }
    var username by remember { mutableStateOf("") }
    var quickConnectCode by remember { mutableStateOf<String?>(null) }
    var quickConnectSecret by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val serverLabel = stringResource(R.string.login_prompt_server)
    val usernameLabel = stringResource(R.string.login_prompt_username)
    val passwordLabel = stringResource(R.string.login_prompt_password)
    val genericError = stringResource(R.string.login_error_generic)
    val quickConnectTimeoutError = stringResource(R.string.login_quick_connect_timeout)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val text = result.data
            ?.let { RemoteInput.getResultsFromIntent(it) }
            ?.getCharSequence(REMOTE_INPUT_KEY)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (text.isEmpty()) {
            errorMessage = genericError
            step = LoginStep.ERROR
            return@rememberLauncherForActivityResult
        }

        when (step) {
            LoginStep.SERVER -> {
                client = session.buildClient(text)
                step = LoginStep.CHECKING
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
                    step = LoginStep.CONNECTING
                    scope.launch {
                        session.login(currentClient, username, text).fold(
                            onSuccess = { onLoggedIn() },
                            onFailure = { error ->
                                errorMessage = error.message ?: genericError
                                step = LoginStep.ERROR
                            },
                        )
                    }
                }
            }
            LoginStep.CHECKING, LoginStep.QUICK_CONNECT, LoginStep.CONNECTING, LoginStep.ERROR -> Unit
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
                            errorMessage = error.message ?: genericError
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
            LoginStep.CONNECTING, LoginStep.ERROR -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (step) {
            LoginStep.CHECKING, LoginStep.CONNECTING -> CircularProgressIndicator()
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
