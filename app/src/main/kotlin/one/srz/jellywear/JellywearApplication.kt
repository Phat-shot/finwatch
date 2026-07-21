package one.srz.jellywear

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache

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
        .build()
}
