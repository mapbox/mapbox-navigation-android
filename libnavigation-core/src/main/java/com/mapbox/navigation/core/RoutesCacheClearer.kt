package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.clearCache
import com.mapbox.navigation.base.internal.clearCacheExceptFor
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutesCacheClearer : RoutesObserver, RoutesPreviewObserver {

    private var currentActiveRoutes: List<NavigationRoute> = emptyList()
    private var currentPreviewsRoutes: List<NavigationRoute>? = null

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        currentActiveRoutes = result.navigationRoutes
        if (result.navigationRoutes.isEmpty() && currentPreviewsRoutes.isNullOrEmpty()) {
            DecodeUtils.clearCache()
        }
        if (result.navigationRoutes.isNotEmpty()) {
            DecodeUtils.clearCacheExceptFor(result.navigationRoutes.map { it.directionsRoute })
        }
    }

    override fun routesPreviewUpdated(update: RoutesPreviewUpdate) {
        currentPreviewsRoutes = update.routesPreview?.routesList
        if (update.routesPreview?.routesList.isNullOrEmpty() && currentActiveRoutes.isEmpty()) {
            DecodeUtils.clearCache()
        }
    }
}
