package one.srz.jellywear.presentation

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlinx.coroutines.delay
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.presentation.components.ProgressRing
import one.srz.jellywear.presentation.home.HomeScreen
import one.srz.jellywear.presentation.library.ArtistAlbumsScreen
import one.srz.jellywear.presentation.library.Category
import one.srz.jellywear.presentation.library.CategoryScreen
import one.srz.jellywear.presentation.library.ItemBrowserScreen
import one.srz.jellywear.playback.EXTRA_OPEN_NOW_PLAYING
import one.srz.jellywear.playback.NowPlaying
import one.srz.jellywear.playback.PlaybackService
import one.srz.jellywear.presentation.login.LoginScreen
import one.srz.jellywear.presentation.player.PLAYER_QUEUE_ID
import one.srz.jellywear.presentation.player.PLAYER_RESUME_ID
import one.srz.jellywear.presentation.player.PlayerScreen
import one.srz.jellywear.presentation.settings.AppearanceSettingsScreen
import one.srz.jellywear.presentation.settings.ColorPickerScreen
import one.srz.jellywear.presentation.settings.ColorPickerTarget
import one.srz.jellywear.presentation.settings.CoverArtModeScreen
import one.srz.jellywear.presentation.settings.LanguageScreen
import one.srz.jellywear.presentation.settings.LibrarySettingsScreen
import one.srz.jellywear.presentation.settings.PlaybackSettingsScreen
import one.srz.jellywear.presentation.settings.SettingsScreen
import one.srz.jellywear.presentation.settings.ThemeModeScreen
import one.srz.jellywear.presentation.theme.JellywearTheme

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"
private const val ROUTE_CATEGORY = "category/{type}"
private const val ROUTE_ARTIST = "artist/{artistId}"
private const val ROUTE_BROWSE = "browse/{parentId}"
private const val ROUTE_PLAYER = "player/{itemId}"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_SETTINGS_APPEARANCE = "settings/appearance"
private const val ROUTE_SETTINGS_PLAYBACK = "settings/playback"
private const val ROUTE_SETTINGS_LIBRARIES = "settings/libraries"
private const val ROUTE_COLOR_PICKER = "colorpicker/{target}"
private const val ROUTE_THEME_MODE = "thememode"
private const val ROUTE_COVER_ART_MODE = "coverartmode"
private const val ROUTE_LANGUAGE = "language"

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Sees only *later* notification/watch-face taps that arrive while the
    // app is already running (onNewIntent below, enabled by singleTask
    // launch mode in the manifest). The very first cold-start tap is handled
    // by openNowPlaying/startDestination in onCreate instead -- this starts
    // false so JellywearApp doesn't also double-navigate on top of that.
    private val openNowPlayingTrigger = mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        val languageTag = AppPreferences.getInstance(newBase).languageTag
        val context = if (languageTag != null) {
            val locale = Locale.forLanguageTag(languageTag)
            val configuration = Configuration(newBase.resources.configuration)
            configuration.setLocale(locale)
            newBase.createConfigurationContext(configuration)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val session = JellyfinSession.getInstance(applicationContext)
        val preferences = AppPreferences.getInstance(applicationContext)
        // Tapping the media notification or the Wear OS watch-face playback
        // icon launches this activity with this extra set, so it should open
        // straight into the now-playing screen instead of the usual home screen.
        val openNowPlaying = intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true
        setContent {
            JellywearApp(
                session = session,
                preferences = preferences,
                openNowPlaying = openNowPlaying,
                openNowPlayingTrigger = openNowPlayingTrigger,
            )
        }
    }

    // With singleTask launch mode, reopening the app -- whether via the
    // launcher icon after audio's idle timeout backgrounds it (moveTaskToBack)
    // or via the notification/watch-face playback icon -- resumes this same
    // instance through here instead of stacking a redundant new one on top,
    // which used to strand the real PlayerScreen one instance back (only
    // reachable by pressing back).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false)) {
            openNowPlayingTrigger.value = true
        }
    }
}

@Composable
fun JellywearApp(
    session: JellyfinSession,
    preferences: AppPreferences,
    openNowPlaying: Boolean = false,
    openNowPlayingTrigger: MutableState<Boolean> = remember { mutableStateOf(false) },
) {
    JellywearTheme(preferences = preferences) {
        val navController = rememberSwipeDismissableNavController()
        val startDestination = when {
            !session.isLoggedIn -> ROUTE_LOGIN
            openNowPlaying -> "player/$PLAYER_RESUME_ID"
            else -> ROUTE_HOME
        }

        // Later notification/watch-face taps while already running (see
        // MainActivity.onNewIntent) -- the cold-start tap is already covered
        // by startDestination above, so this only fires for taps *after*
        // that, each time consuming the trigger so it doesn't refire.
        LaunchedEffect(openNowPlayingTrigger.value) {
            if (openNowPlayingTrigger.value) {
                if (session.isLoggedIn) {
                    navController.navigate("player/$PLAYER_RESUME_ID")
                }
                openNowPlayingTrigger.value = false
            }
        }

        // A single MediaController connection dedicated to the ring overlay
        // below, kept alive app-wide (not just while PlayerScreen is
        // mounted) so the ring can show progress and seek from any screen.
        // Gated on NowPlaying.isActive so it only ever binds PlaybackService
        // once playback has actually started -- otherwise just opening the
        // app would spin up the service and its notification for nothing.
        // PlayerScreen keeps its own separate connection for the delicate
        // video foreground/background lifecycle handling; MediaSession
        // supports multiple simultaneous controllers, so the two don't
        // conflict.
        val context = LocalContext.current
        var ringController by remember { mutableStateOf<MediaController?>(null) }
        DisposableEffect(NowPlaying.isActive) {
            if (!NowPlaying.isActive) {
                return@DisposableEffect onDispose { }
            }
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            future.addListener(
                {
                    ringController = try {
                        future.get()
                    } catch (e: CancellationException) {
                        null
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
            onDispose {
                MediaController.releaseFuture(future)
                ringController = null
            }
        }
        LaunchedEffect(ringController) {
            val ctrl = ringController ?: return@LaunchedEffect
            while (true) {
                NowPlaying.positionMs = ctrl.currentPosition.coerceAtLeast(0L)
                NowPlaying.durationMs = ctrl.duration.coerceAtLeast(0L)
                NowPlaying.isPlaying = ctrl.isPlaying
                delay(500)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = startDestination,
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
                        preferences = preferences,
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
                            onSkipToArtist = { id ->
                                navController.navigate("artist/$id") {
                                    popUpTo(ROUTE_CATEGORY) { inclusive = true }
                                }
                            },
                            onSkipToFolder = { id ->
                                navController.navigate("browse/$id") {
                                    popUpTo(ROUTE_CATEGORY) { inclusive = true }
                                }
                            },
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
                            onSkipToAlbum = { id ->
                                navController.navigate("browse/$id") {
                                    popUpTo(ROUTE_ARTIST) { inclusive = true }
                                }
                            },
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
                        PlayerScreen(session = session, preferences = preferences, itemId = itemId)
                    }
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        session = session,
                        onOpenAppearance = { navController.navigate(ROUTE_SETTINGS_APPEARANCE) },
                        onOpenPlayback = { navController.navigate(ROUTE_SETTINGS_PLAYBACK) },
                        onOpenLibraries = { navController.navigate(ROUTE_SETTINGS_LIBRARIES) },
                        onLoggedOut = {
                            navController.navigate(ROUTE_LOGIN) {
                                popUpTo(ROUTE_HOME) { inclusive = true }
                            }
                        },
                    )
                }
                composable(ROUTE_SETTINGS_APPEARANCE) {
                    AppearanceSettingsScreen(
                        preferences = preferences,
                        onOpenThemeModePicker = { navController.navigate(ROUTE_THEME_MODE) },
                        onOpenCoverArtModePicker = { navController.navigate(ROUTE_COVER_ART_MODE) },
                        onOpenAccentColorPicker = { navController.navigate("colorpicker/${ColorPickerTarget.ACCENT.route}") },
                        onOpenFontColorPicker = { navController.navigate("colorpicker/${ColorPickerTarget.FONT.route}") },
                        onOpenLanguagePicker = { navController.navigate(ROUTE_LANGUAGE) },
                    )
                }
                composable(ROUTE_SETTINGS_PLAYBACK) {
                    PlaybackSettingsScreen(preferences = preferences)
                }
                composable(ROUTE_SETTINGS_LIBRARIES) {
                    LibrarySettingsScreen(preferences = preferences)
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
                composable(ROUTE_THEME_MODE) {
                    ThemeModeScreen(
                        preferences = preferences,
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(ROUTE_COVER_ART_MODE) {
                    CoverArtModeScreen(
                        preferences = preferences,
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(ROUTE_LANGUAGE) {
                    LanguageScreen(preferences = preferences)
                }
            }

            if (NowPlaying.isActive) {
                val duration = NowPlaying.durationMs
                val progress = if (duration > 0) {
                    (NowPlaying.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                ProgressRing(
                    progress = progress,
                    onSeek = { fraction ->
                        val ctrl = ringController
                        val seekDuration = ctrl?.duration?.takeIf { it > 0 } ?: duration
                        if (ctrl != null && seekDuration > 0) {
                            val target = (fraction * seekDuration).toLong()
                            ctrl.seekTo(target)
                            NowPlaying.positionMs = target
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
