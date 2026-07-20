package one.srz.jellywear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.presentation.library.LibraryScreen
import one.srz.jellywear.presentation.login.LoginScreen
import one.srz.jellywear.presentation.theme.JellywearTheme

private const val ROUTE_LOGIN = "login"
private const val ROUTE_LIBRARY = "library"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = JellyfinSession.getInstance(applicationContext)
        setContent {
            JellywearApp(session = session)
        }
    }
}

@Composable
fun JellywearApp(session: JellyfinSession) {
    JellywearTheme {
        val navController = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = if (session.isLoggedIn) ROUTE_LIBRARY else ROUTE_LOGIN,
        ) {
            composable(ROUTE_LOGIN) {
                LoginScreen(
                    session = session,
                    onLoggedIn = {
                        navController.navigate(ROUTE_LIBRARY) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(ROUTE_LIBRARY) {
                LibraryScreen(session = session)
            }
        }
    }
}
