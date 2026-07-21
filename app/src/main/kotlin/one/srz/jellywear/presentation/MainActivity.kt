package one.srz.jellywear.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.presentation.home.HomeScreen
import one.srz.jellywear.presentation.library.ArtistAlbumsScreen
import one.srz.jellywear.presentation.library.Category
import one.srz.jellywear.presentation.library.CategoryScreen
import one.srz.jellywear.presentation.library.ItemBrowserScreen
import one.srz.jellywear.presentation.login.LoginScreen
import one.srz.jellywear.presentation.player.PLAYER_QUEUE_ID
import one.srz.jellywear.presentation.player.PlayerScreen
import one.srz.jellywear.presentation.settings.ColorPickerScreen
import one.srz.jellywear.presentation.settings.ColorPickerTarget
import one.srz.jellywear.presentation.settings.SettingsScreen
import one.srz.jellywear.presentation.theme.JellywearTheme

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"
private const val ROUTE_CATEGORY = "category/{type}"
private const val ROUTE_ARTIST = "artist/{artistId}"
private const val ROUTE_BROWSE = "browse/{parentId}"
private const val ROUTE_PLAYER = "player/{itemId}"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_COLOR_PICKER = "colorpicker/{target}"

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val session = JellyfinSession.getInstance(applicationContext)
        val preferences = AppPreferences.getInstance(applicationContext)
        setContent {
            JellywearApp(session = session, preferences = preferences)
        }
    }
}

@Composable
fun JellywearApp(session: JellyfinSession, preferences: AppPreferences) {
    JellywearTheme(preferences = preferences) {
        val navController = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = if (session.isLoggedIn) ROUTE_HOME else ROUTE_LOGIN,
        ) {
            composable(ROUTE_LOGIN) {
                LoginScreen(
                    session = session,
                    onLoggedIn = {
                        navController.navigate(ROUTE_HOME) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(ROUTE_HOME) {
                HomeScreen(
                    session = session,
                    onOpenCategory = { category -> navController.navigate("category/${category.route}") },
                    onShufflePlay = { navController.navigate("player/$PLAYER_QUEUE_ID") },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                )
            }
            composable(ROUTE_CATEGORY) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("type")?.let(Category::fromRoute)
                if (category != null) {
                    CategoryScreen(
                        session = session,
                        preferences = preferences,
                        category = category,
                        onOpenArtist = { id -> navController.navigate("artist/$id") },
                        onOpenFolder = { id -> navController.navigate("browse/$id") },
                        onPlayItem = { id -> navController.navigate("player/$id") },
                        onShufflePlay = { navController.navigate("player/$PLAYER_QUEUE_ID") },
                    )
                }
            }
            composable(ROUTE_ARTIST) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId")
                if (artistId != null) {
                    ArtistAlbumsScreen(
                        session = session,
                        preferences = preferences,
                        artistId = artistId,
                        onOpenAlbum = { id -> navController.navigate("browse/$id") },
                        onShufflePlay = { navController.navigate("player/$PLAYER_QUEUE_ID") },
                    )
                }
            }
            composable(ROUTE_BROWSE) { backStackEntry ->
                val parentId = backStackEntry.arguments?.getString("parentId")
                if (parentId != null) {
                    ItemBrowserScreen(
                        session = session,
                        preferences = preferences,
                        parentId = parentId,
                        onOpenFolder = { id -> navController.navigate("browse/$id") },
                        onPlayItem = { id -> navController.navigate("player/$id") },
                        onShufflePlay = { navController.navigate("player/$PLAYER_QUEUE_ID") },
                    )
                }
            }
            composable(ROUTE_PLAYER) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                if (itemId != null) {
                    PlayerScreen(session = session, itemId = itemId)
                }
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    session = session,
                    preferences = preferences,
                    onOpenAccentColorPicker = { navController.navigate("colorpicker/${ColorPickerTarget.ACCENT.route}") },
                    onOpenFontColorPicker = { navController.navigate("colorpicker/${ColorPickerTarget.FONT.route}") },
                    onLoggedOut = {
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(ROUTE_HOME) { inclusive = true }
                        }
                    },
                )
            }
            composable(ROUTE_COLOR_PICKER) { backStackEntry ->
                val target = backStackEntry.arguments?.getString("target")?.let(ColorPickerTarget::fromRoute)
                if (target != null) {
                    ColorPickerScreen(
                        target = target,
                        preferences = preferences,
                        onDone = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
