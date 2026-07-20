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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            val versionCode = output.versionCode.orNull ?: 1
            val suffix = if (variant.flavorName == "beta") "-test" else ""
            output.outputFileName.set("jellywear-v$versionCode$suffix.apk")
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
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.wear.compose:compose-material:1.3.1")
    implementation("androidx.wear.compose:compose-foundation:1.3.1")

    // Media3 for future Jellyfin audio/video playback (ExoPlayer + MediaSession).
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")

    testImplementation("junit:junit:4.13.2")
}
