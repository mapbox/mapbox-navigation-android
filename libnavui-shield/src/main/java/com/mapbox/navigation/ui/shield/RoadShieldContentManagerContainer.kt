package com.mapbox.navigation.ui.shield

import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload

/**
 * Container for [RoadShieldContentManager] implementation which holds it in a static context,
 * so that the same cache can be reused through the app processes life.
 */
internal object RoadShieldContentManagerContainer : RoadShieldContentManager {
    private val contentManager: RoadShieldContentManager by lazy {
        RoadShieldContentManagerImpl()
    }

    override suspend fun getShields(shieldsToDownload: List<RouteShieldToDownload>) =
        contentManager.getShields(shieldsToDownload)

    override fun cancelAll() = contentManager.cancelAll()
}
