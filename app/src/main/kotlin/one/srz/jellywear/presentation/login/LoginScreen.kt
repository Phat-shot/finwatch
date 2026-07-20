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
import kotlinx.coroutines.launch
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession

private const val REMOTE_INPUT_KEY = "text"

private enum class LoginStep { SERVER, USERNAME, PASSWORD, CONNECTING, ERROR }

@Composable
fun LoginScreen(
    session: JellyfinSession,
    onLoggedIn: () -> Unit,
) {
    var step by remember { mutableStateOf(LoginStep.SERVER) }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val serverLabel = stringResource(R.string.login_prompt_server)
    val usernameLabel = stringResource(R.string.login_prompt_username)
    val passwordLabel = stringResource(R.string.login_prompt_password)
    val genericError = stringResource(R.string.login_error_generic)

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
                serverUrl = text
                step = LoginStep.USERNAME
            }
            LoginStep.USERNAME -> {
                username = text
                step = LoginStep.PASSWORD
            }
            LoginStep.PASSWORD -> {
                step = LoginStep.CONNECTING
                scope.launch {
                    session.login(serverUrl, username, text).fold(
                        onSuccess = { onLoggedIn() },
                        onFailure = { error ->
                            errorMessage = error.message ?: genericError
                            step = LoginStep.ERROR
                        },
                    )
                }
            }
            LoginStep.CONNECTING, LoginStep.ERROR -> Unit
        }
    }

    LaunchedEffect(step) {
        val label = when (step) {
            LoginStep.SERVER -> serverLabel
            LoginStep.USERNAME -> usernameLabel
            LoginStep.PASSWORD -> passwordLabel
            LoginStep.CONNECTING, LoginStep.ERROR -> null
        }
        if (label != null) {
            launcher.launch(buildRemoteInputIntent(label))
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
            LoginStep.CONNECTING -> CircularProgressIndicator()
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
