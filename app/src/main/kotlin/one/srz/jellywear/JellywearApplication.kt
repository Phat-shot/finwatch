package one.srz.jellywear

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import one.srz.jellywear.data.JellyfinSession

/**
 * Configures Coil's global ImageLoader for Wear OS's constrained RAM --
 * Coil's default memory cache (25% of available app memory) is tuned for
 * phones, not a watch. Also enables crossfade so cover art thumbnails pop
 * in smoothly instead of causing a layout jump.
 */
class JellywearApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.1)
                .build()
        }
        // Cover-art URLs are token-free (see JellyfinSession.imageUrl) --
        // requests to the Jellyfin server are authenticated with the
        // Authorization header instead, so the token stays out of logs and
        // Coil's disk cache keys. Only sent to the session's own server
        // host, never to anything else Coil might ever load.
        .okHttpClient {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val session = JellyfinSession.getInstance(this)
                    val serverHost = session.serverUrl?.toHttpUrlOrNull()?.host
                    val header = session.authorizationHeader()
                    val request = chain.request()
                    if (header != null && serverHost != null && request.url.host == serverHost) {
                        chain.proceed(request.newBuilder().header("Authorization", header).build())
                    } else {
                        chain.proceed(request)
                    }
                }
                .build()
        }
        .build()
}
