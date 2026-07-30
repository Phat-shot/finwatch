plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "one.srz.jellywear"
    compileSdk = 34

    defaultConfig {
        applicationId = "one.srz.jellywear"
        minSdk = 28
        targetSdk = 34

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
            // applicationId / app_name come from the main source set (jellywear).
        }
        create("beta") {
            dimension = "channel"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            // app_name ("jellywear beta") and badged launcher icons live in
            // src/beta/res, see scripts/badge_launcher_icon.py.
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    // AnimatedVisibility/fadeIn/fadeOut for the progress ring's fade with the
    // video controls -- pulled in transitively by compose-navigation already,
    // declared explicitly since that's an implementation detail to rely on.
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // 1.4.0 for ScalingLazyColumn's built-in rotaryScrollableBehavior (crown scrolling).
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear.compose:compose-navigation:1.4.0")
    implementation("androidx.wear:wear-input:1.2.0")

    // Cover/album art thumbnails.
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Jellyfin server API client.
    implementation("org.jellyfin.sdk:jellyfin-core:1.8.6")
    // jellyfin-core's HTTP client logs via kotlin-logging's SLF4J backend,
    // which needs a binding on the classpath or it crashes with
    // NoClassDefFoundError on Android (no SLF4J implementation by default).
    // slf4j-simple writes to stdout/stderr, which logcat captures.
    implementation("org.slf4j:slf4j-simple:2.0.17")

    // Media3 for Jellyfin audio/video playback (ExoPlayer + MediaSession + video surface).
    // Pinned below 1.5.0: from there on media3 requires compileSdk 35+/36+,
    // ahead of what AGP 8.5.2 (max recommended compileSdk 34) supports here.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    testImplementation("junit:junit:4.13.2")
}
