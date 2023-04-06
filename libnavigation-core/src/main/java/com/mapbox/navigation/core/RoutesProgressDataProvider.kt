package com.mapbox.navigation.core

import androidx.annotation.MainThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal data class RoutesProgressData(
    val primary: RouteProgressData,
    val alternatives: Map<String, RouteProgressData>,
)

/**
 * Accumulates and provides route refresh model data from different sources.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@MainThread
internal class RoutesProgressDataProvider : RouteProgressObserver {

    private val defaultRouteProgressData = RouteProgressData(0, 0, null)
    private var routesProgressData: RoutesProgressData? = null
    private var continuation: CancellableContinuation<RoutesProgressData>? = null

    /**
     * Returns either last saved value (if has one) or waits for the next update.
     */
    suspend fun getRouteRefreshRequestDataOrWait(): RoutesProgressData {
        return (routesProgressData ?: suspendCancellableCoroutine { continuation = it })
    }

    fun onNewRoutes() {
        routesProgressData = null
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val primary = RouteProgressData(
            legIndex = routeProgress.currentLegProgress?.legIndex
                ?: defaultRouteProgressData.legIndex,
            routeGeometryIndex = routeProgress.currentRouteGeometryIndex,
            legGeometryIndex = routeProgress.currentLegProgress?.geometryIndex,
        )
        val alternatives = routeProgress.internalAlternativeRouteIndices().entries.associate {
            it.key to RouteProgressData(
                it.value.legIndex,
                it.value.routeGeometryIndex,
                it.value.legGeometryIndex
            )
        }
        routesProgressData = RoutesProgressData(primary, alternatives).also {
            continuation?.resume(it)
            continuation = null
        }
    }
}
