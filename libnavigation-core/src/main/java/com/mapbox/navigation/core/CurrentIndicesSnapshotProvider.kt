package com.mapbox.navigation.core

import androidx.annotation.MainThread
import com.mapbox.navigation.base.internal.CurrentIndicesSnapshot
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Gets updates from [RouteProgress], converts them into [CurrentIndicesSnapshot]
 * and provides said info.
 * Supports freezing and unfreezing of updates, see [getFilledIndicesAndFreeze] and [unfreeze] for details.
 */
@MainThread
internal class CurrentIndicesSnapshotProvider :
    RouteProgressObserver, Function0<CurrentIndicesSnapshot?> {

    private val defaultIndicesSnapshot = CurrentIndicesSnapshot()
    private var frozen = false
    private var indicesSnapshot: CurrentIndicesSnapshot? = null
    private var continuation: CancellableContinuation<CurrentIndicesSnapshot>? = null

    /**
     * Returns either last saved value (if has one) or waits for the next update,
     * and freezes the upcoming updates.
     * Updates will be frozen, meaning they will not affect
     * [getFilledIndicesAndFreeze] and [invoke] results.
     */
    suspend fun getFilledIndicesAndFreeze(): CurrentIndicesSnapshot {
        return (indicesSnapshot ?: suspendCancellableCoroutine { continuation = it })
            .also { frozen = true }
    }

    /**
     * Unfreezes updates.
     */
    fun unfreeze() {
        frozen = false
    }

    /**
     * Resets saved info to null.
     */
    fun clear() {
        indicesSnapshot = null
    }

    /**
     * Update indices. Will not update in frozen state.
     */
    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        if (!frozen) {
            indicesSnapshot = CurrentIndicesSnapshot(
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

    /**
     * Returns last saved info.
     */
    override fun invoke(): CurrentIndicesSnapshot? = indicesSnapshot
}
