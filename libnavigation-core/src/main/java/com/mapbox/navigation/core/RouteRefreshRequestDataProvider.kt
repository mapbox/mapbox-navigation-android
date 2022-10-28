package com.mapbox.navigation.core

import androidx.annotation.MainThread
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routerefresh.EVDataHolder
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal data class RouteProgressData(
    val legIndex: Int,
    val routeGeometryIndex: Int,
    val legGeometryIndex: Int?,
)

/**
 * Accumulates and provides route refresh model data from different sources.
 */
@MainThread
internal class RouteRefreshRequestDataProvider(
    private val evDataHolder: EVDataHolder = EVDataHolder()
) : RouteProgressObserver {

    private val defaultRouteProgressData = RouteProgressData(0, 0, null)
    private var routeProgressData: RouteProgressData? = null
    private var continuation: CancellableContinuation<RouteProgressData>? = null

    /**
     * Returns either last saved value (if has one) or waits for the next update.
     */
    suspend fun getRouteRefreshRequestDataOrWait(): RouteRefreshRequestData {
        return (routeProgressData ?: suspendCancellableCoroutine { continuation = it }).let {
            RouteRefreshRequestData(
                it.legIndex,
                it.routeGeometryIndex,
                it.legGeometryIndex,
                evDataHolder.currentData()
            )
        }
    }

    /**
     * Resets saved route data info to null.
     */
    fun onNewRoute() {
        routeProgressData = null
    }

    fun onEVDataUpdated(data: Map<String, String?>) {
        evDataHolder.onEVDataUpdated(data)
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        routeProgressData = RouteProgressData(
            legIndex = routeProgress.currentLegProgress?.legIndex
                ?: defaultRouteProgressData.legIndex,
            routeGeometryIndex = routeProgress.currentRouteGeometryIndex,
            legGeometryIndex = routeProgress.currentLegProgress?.geometryIndex,
        ).also {
            continuation?.resume(it)
            continuation = null
        }
    }
}
