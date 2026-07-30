# R8 rules for Finwatch release builds.
#
# Most dependencies bring their own rules, applied automatically by AGP:
#   - kotlinx.serialization embeds R8 rules in its jars
#     (META-INF/com.android.tools/r8/kotlinx-serialization-*.pro; verified
#     present in kotlinx-serialization-core 1.9.0): keeps Companion fields
#     and serializer() members of @Serializable classes.
#   - jellyfin-core-android 1.8.12 ships consumer rules in its AAR
#     (proguard.txt), see the jellyfin-sdk section below.
#   - okhttp-android 5.x (the SDK's HTTP client -- the Android variant of
#     jellyfin-sdk 1.8.12 uses OkHttp, not Ktor; verified in its POM) ships
#     consumer -dontwarn rules for JVM-only platform classes; okio embeds
#     rules in its jar as well.
#   - Media3 and the AndroidX/Compose/Wear-Compose libraries ship consumer
#     proguard.txt files in their AARs (standard AndroidX practice; e.g.
#     media3-exoplayer keeps its reflectively-constructed components).
#   - Coil 2.x needs no rules at all -- R8-compatible by design (per the
#     Coil FAQ; verified: coil-base 2.7.0 contains no proguard.txt).
#
# The rules below cover the gaps the consumer rules leave, erring on the
# side of keeping too much rather than risking a runtime crash that only
# shows up on a watch.

########################################################################
# Debuggability: keep file/line info so release stack traces (Play
# Console, adb bugreports) stay mappable with the R8 mapping file, while
# hiding the original source paths behind a generic "SourceFile".
########################################################################
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

########################################################################
# jellyfin-sdk-kotlin 1.8.x + kotlinx.serialization
#
# The SDK's own consumer rules are:
#   -keep class org.jellyfin.sdk.model.**.* { *; }
#   -keep class org.jellyfin.sdk.api.client.exception.**.* { *; }
# The trailing "**.*" pattern is aimed at subpackages; to be safe we
# broaden it to the whole packages so classes sitting directly in
# org.jellyfin.sdk.model (e.g. ClientInfo, DeviceInfo, the UUID
# serializer helpers) are guaranteed to survive too. Keeping the DTOs
# wholesale also keeps their generated kotlinx.serialization
# `$serializer` / `Companion` classes, which the SDK resolves at runtime
# when (de)serializing API responses -- if those get stripped or renamed,
# every API call crashes with a SerializationException at runtime.
########################################################################
-keep class org.jellyfin.sdk.model.** { *; }
-keep class org.jellyfin.sdk.api.client.exception.** { *; }

# Standard kotlinx.serialization lookup rules (from the library's README).
# Mostly redundant with the rules embedded in the serialization artifacts,
# repeated here defensively in case a dependency update ever drops them:
# serializers are resolved through Companion objects and generated
# `$$serializer` INSTANCEs, partly via reflection.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class org.jellyfin.sdk.** {
    *** Companion;
}
-keepclasseswithmembers class org.jellyfin.sdk.** {
    kotlinx.serialization.KSerializer serializer(...);
}

########################################################################
# slf4j (slf4j-api + slf4j-simple)
#
# slf4j-api 2.x discovers its backend (slf4j-simple's
# SimpleServiceProvider) via java.util.ServiceLoader. R8 keeps
# ServiceLoader providers only as long as it can match the
# META-INF/services entry to the class -- pin the provider interface's
# implementations explicitly so the binding can't be stripped, which
# would silently disable logging and trip slf4j's "no provider" path.
# JellywearApplication also configures it purely via System properties
# (string keys), which is obfuscation-safe.
########################################################################
-keep class * implements org.slf4j.spi.SLF4JServiceProvider { *; }
# slf4j-api probes optional JVM-only integrations that don't exist on
# Android; don't fail the build over them.
-dontwarn org.slf4j.**

########################################################################
# OkHttp / Okio (jellyfin-sdk internal HTTP stack)
#
# Their own consumer rules already contain these -dontwarn lines for
# JVM/desktop-only classes (Conscrypt, BouncyCastle, ...); repeated here
# defensively -- they are no-ops when already present and keep the build
# green if a future SDK update swaps the artifacts around.
########################################################################
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

########################################################################
# App code
#
# No reflection, no JNI, no @Keep-needing entry points beyond what AGP
# already keeps automatically (manifest components, Compose runtime via
# its own consumer rules). Nothing to add -- deliberately no blanket
# "-keep class one.srz.jellywear.**" so the app's own code actually gets
# shrunk and obfuscated.
########################################################################
