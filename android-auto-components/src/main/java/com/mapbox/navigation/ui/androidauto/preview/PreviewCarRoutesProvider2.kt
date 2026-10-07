package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.common.dispatchers.SdkDispatchers
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.ui.androidauto.internal.extensions.selectPreviewedRoute
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.navigation.CarNavigationCamera
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/**
 * Gives the [CarRoutePreviewScreen] the ability to control the selected route rendered
 * by the [CarRouteLineRenderer] and [CarNavigationCamera].
 */
@ExperimentalPreviewMapboxNavigationAPI
class PreviewCarRoutesProvider2 : CarRoutesProvider {

    // Upstream runs only while there are collectors, so this scope never runs work on its own.
    private val sharingScope = CoroutineScope(SupervisorJob() + SdkDispatchers.Main.immediate)

    /**
     * Provides navigation routes and selected route index for route preview.
     *
     * All collectors share one routes preview observer. The latest preview is replayed to new
     * collectors while the flow is collected, and dropped when the last collector stops.
     */
    val routesPreview: Flow<RoutesPreview?> = callbackFlow {
        val routesObserver = RoutesPreviewObserver { trySend(it.routesPreview) }
        val observer = object : MapboxNavigationObserver {

            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerRoutesPreviewObserver(routesObserver)
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterRoutesPreviewObserver(routesObserver)
                trySend(element = null)
            }
        }
        MapboxNavigationApp.registerObserver(observer)
        awaitClose {
            MapboxNavigationApp.unregisterObserver(observer)
        }
    }.shareIn(
        sharingScope,
        SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
        replay = 1,
    )

    override val navigationRoutes = routesPreview.map { it?.routesList.orEmpty() }

    /**
     * When the route selection changes, update the route selection. This will relay the route
     * selection to other components.
     */
    fun updateSelectedRoute(index: Int) {
        val mapboxNavigation = MapboxNavigationApp.current() ?: run {
            logAndroidAutoFailure(
                "PreviewCarRoutesProvider2 updateSelectedRoute($index) ignored, " +
                    "MapboxNavigation is detached",
            )
            return
        }
        // Resolve the index against the current preview: the template may have been built from
        // a preview that was cleared or replaced before the selection arrived.
        val route = mapboxNavigation.getRoutesPreview()?.originalRoutesList?.getOrNull(index)
            ?: run {
                logAndroidAutoFailure(
                    "PreviewCarRoutesProvider2 updateSelectedRoute($index) ignored, " +
                        "no such previewed route",
                )
                return
            }
        mapboxNavigation.changeRoutesPreviewPrimaryRoute(route)
    }

    /**
     * Makes the previewed route with [routeId] the primary previewed route. Unlike
     * [updateSelectedRoute], a selection made on a template built for an older preview can't pick
     * a route from a newer preview.
     *
     * @return true when the selection was requested for a route in the current preview.
     */
    internal fun selectRoute(routeId: String): Boolean {
        val mapboxNavigation = MapboxNavigationApp.current() ?: run {
            logAndroidAutoFailure(
                "PreviewCarRoutesProvider2 selectRoute ignored, MapboxNavigation is detached",
            )
            return false
        }
        val selected = mapboxNavigation.selectPreviewedRoute(routeId)
        if (!selected) {
            logAndroidAutoFailure(
                "PreviewCarRoutesProvider2 selectRoute ignored, route $routeId is not previewed",
            )
        }
        return selected
    }
}
