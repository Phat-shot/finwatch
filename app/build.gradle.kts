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
            // TODO: replace with a real release keystore (via GitHub Secrets)
            // once we're ready to ship signed builds. Debug signing keeps
            // both branches producing an installable APK in the meantime.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.wear.compose:compose-material:1.3.1")
    implementation("androidx.wear.compose:compose-foundation:1.3.1")
    implementation("androidx.wear.compose:compose-navigation:1.3.1")
    implementation("androidx.wear:wear-input:1.2.0")

    // Jellyfin server API client.
    implementation("org.jellyfin.sdk:jellyfin-core:1.8.6")

    // Media3 for Jellyfin audio/video playback (ExoPlayer + MediaSession + video surface).
    // Pinned below 1.5.0: from there on media3 requires compileSdk 35+/36+,
    // ahead of what AGP 8.5.2 (max recommended compileSdk 34) supports here.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    testImplementation("junit:junit:4.13.2")
}
