plugins {
    // Latest 8.x line (needs Gradle >= 8.13, JDK >= 17, supports
    // compileSdk 36). AGP 9.x is out but is a separate migration
    // (Gradle 9, built-in Kotlin support) -- see issue #21, stage 2.
    id("com.android.application") version "8.13.2" apply false
    // Kotlin 2.3.x is the newest line AGP 8.13.2 documents support for
    // (via R8 8.13.19); Kotlin 2.4.x is reserved for the AGP 9 migration.
    // The Compose compiler plugin is versioned in lockstep with Kotlin.
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
