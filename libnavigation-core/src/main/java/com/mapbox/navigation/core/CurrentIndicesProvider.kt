package com.mapbox.navigation.core

import androidx.annotation.MainThread
import com.mapbox.navigation.base.CurrentIndices
import com.mapbox.navigation.base.internal.CurrentIndicesFactory
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Gets updates from [RouteProgress], converts them into [CurrentIndices]
 * and provides said info.
 */
@MainThread
internal class CurrentIndicesProvider : RouteProgressObserver {

    private val defaultIndicesSnapshot = CurrentIndicesFactory.createIndices(0, 0, null)
    private var indicesSnapshot: CurrentIndices? = null
    private var continuation: CancellableContinuation<CurrentIndices>? = null

    /**
     * Returns either last saved value (if has one) or waits for the next update.
     */
    suspend fun getFilledIndicesOrWait(): CurrentIndices {
        return (indicesSnapshot ?: suspendCancellableCoroutine { continuation = it })
    }

    /**
     * Resets saved info to null.
     */
    fun clear() {
        indicesSnapshot = null
    }

    /**
     * Updates indices.
     */
    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        indicesSnapshot = CurrentIndicesFactory.createIndices(
            legIndex = routeProgress.currentLegProgress?.legIndex
                ?: defaultIndicesSnapshot.legIndex,
            routeGeometryIndex = routeProgress.currentRouteGeometryIndex,
            legGeometryIndex = routeProgress.currentLegProgress?.geometryIndex,
        ).also {
            continuation?.resume(it)
            continuation = null
        }
    }
}
