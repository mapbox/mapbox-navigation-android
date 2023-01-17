package com.mapbox.navigation.ui.shield

import com.mapbox.navigation.ui.shield.internal.RoadShieldDownloader
import com.mapbox.navigation.ui.shield.internal.loader.CachedLoader
import com.mapbox.navigation.ui.shield.internal.loader.RoadShieldLoader
import com.mapbox.navigation.ui.shield.internal.loader.ShieldSpritesDownloader
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload

/**
 * Container for [RoadShieldContentManager] implementation which holds it in a static context,
 * so that the same cache can be reused through the app processes life.
 */
internal object RoadShieldContentManagerContainer : RoadShieldContentManager {
    private const val SPRITES_CACHE_SIZE = 8 // entries
    private const val IMAGES_CACHE_SIZE = 40 // entries

    private val contentManager: RoadShieldContentManager by lazy {
        RoadShieldContentManagerImpl(
            shieldLoader = CachedLoader(
                IMAGES_CACHE_SIZE,
                RoadShieldLoader(
                    spritesLoader = CachedLoader(
                        SPRITES_CACHE_SIZE,
                        ShieldSpritesDownloader()
                    ),
                    imageLoader = { url ->
                        RoadShieldDownloader.download(url)
                    }
                )
            )
        )
    }

    override suspend fun getShields(shieldsToDownload: List<RouteShieldToDownload>) =
        contentManager.getShields(shieldsToDownload)

    override fun cancelAll() = contentManager.cancelAll()
}
