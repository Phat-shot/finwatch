plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "one.srz.jellywear"
    compileSdk = 36

    defaultConfig {
        // App is branded "Finwatch"; the namespace / Kotlin packages keep the
        // historical one.srz.jellywear -- invisible to users and Play, and a
        // package-wide rename buys nothing but churn. The applicationId is
        // what Play and devices identify the app by, and it is forever once
        // the first release ships, so it carries the real name.
        applicationId = "one.srz.finwatch"
        minSdk = 28
        // 36 (not just Play's current minimum of 35): from 2026-08-31 Play
        // requires targetSdk 36 for new apps and updates anyway, and none of
        // the Android 15/16 behavior changes bite this app -- no
        // onBackPressed/KEYCODE_BACK handling (predictive back is a no-op
        // with swipe-dismiss Compose navigation), no BODY_SENSORS, and
        // edge-to-edge enforcement targets phones/tablets, not the always
        // fullscreen Wear surface. The mediaPlayback foreground service
        // type + FOREGROUND_SERVICE_MEDIA_PLAYBACK permission required
        // since targetSdk 34 are already declared in the manifest.
        targetSdk = 36

        val ciVersionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull()
        versionCode = ciVersionCode ?: 1
        versionName = "1.${versionCode}"
    }

    signingConfigs {
        getByName("debug") {
            // Fixed, committed keystore instead of the machine-local default
            // (~/.android/debug.keystore) -- otherwise every CI run signs
            // with a different key and Android refuses to install an update
            // over a previous build ("signatures do not match").
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Real release signing, fed entirely from the environment (CI
        // secrets, see .github/workflows/build.yml) or Gradle properties
        // (e.g. ~/.gradle/gradle.properties locally) -- no key material in
        // the repo. If RELEASE_KEYSTORE_FILE isn't set, the release build
        // type below falls back to the debug key so forks and secretless CI
        // runs still produce an installable artifact.
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_FILE")
                ?: project.findProperty("releaseKeystoreFile") as String?
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: project.findProperty("releaseKeystorePassword") as String?
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                    ?: project.findProperty("releaseKeyAlias") as String?
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                    ?: project.findProperty("releaseKeyPassword") as String?
            }
        }
    }

    flavorDimensions += "channel"
    productFlavors {
        create("prod") {
            dimension = "channel"
            // applicationId / app_name come from the main source set (Finwatch).
        }
        create("beta") {
            dimension = "channel"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            // app_name ("Finwatch beta") and badged launcher icons live in
            // src/beta/res, see scripts/badge_launcher_icon.py.
        }
    }

    buildTypes {
        release {
            // R8 code shrinking + resource shrinking: smaller APK/AAB and
            // less bytecode surface. Keep rules for the libraries that need
            // them (jellyfin-sdk/kotlinx.serialization, slf4j) live in
            // proguard-rules.pro; most other dependencies ship their own
            // consumer rules inside their artifacts (see comments there).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Real keystore when the RELEASE_* environment/properties are
            // configured (CI secrets), debug signing otherwise so every
            // checkout still builds an installable APK without any setup.
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Replaces the deprecated android.kotlinOptions DSL (slated for removal in
// newer Kotlin Gradle plugin versions).
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // core 1.18+/lifecycle 2.10+ require AGP 9.1 / compileSdk 37 -- these are
    // the newest versions the current AGP 8.13 / compileSdk 36 toolchain
    // accepts (checkAarMetadata fails the build otherwise).
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")

    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Pinned explicitly: the icon artifacts stopped at 1.7.8 (Compose 1.8
    // dropped them) and are no longer managed by newer Compose BOMs. 1.7.8
    // stays binary-compatible with current Compose runtimes.
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // AnimatedVisibility/fadeIn/fadeOut for the progress ring's fade with the
    // video controls -- pulled in transitively by compose-navigation already,
    // declared explicitly since that's an implementation detail to rely on.
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.wear.compose:compose-material:1.6.2")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.wear.compose:compose-navigation:1.6.2")
    implementation("androidx.wear:wear-input:1.2.0")

    // Cover/album art thumbnails. 2.7.0 is the last Coil 2 release; Coil 3
    // is a separate migration (new io.coil-kt.coil3 coordinates + package
    // names, ImageLoaderFactory replaced by SingletonImageLoader.Factory,
    // OkHttp moved behind coil-network-okhttp) -- deliberately not done
    // here, see https://coil-kt.github.io/coil/upgrading_to_coil3/.
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Jellyfin server API client. 1.8.12 is the latest stable; 1.9.x is
    // still in beta and raises the minimum supported server to Jellyfin 12,
    // which would cut off users on older servers.
    implementation("org.jellyfin.sdk:jellyfin-core:1.8.12")
    // jellyfin-core's HTTP client logs via kotlin-logging's SLF4J backend,
    // which needs a binding on the classpath or it crashes with
    // NoClassDefFoundError on Android (no SLF4J implementation by default).
    // slf4j-simple writes to stdout/stderr, which logcat captures.
    implementation("org.slf4j:slf4j-simple:2.0.18")

    // Media3 for Jellyfin audio/video playback (ExoPlayer + MediaSession + video surface).
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-session:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")

    testImplementation("junit:junit:4.13.2")
}
