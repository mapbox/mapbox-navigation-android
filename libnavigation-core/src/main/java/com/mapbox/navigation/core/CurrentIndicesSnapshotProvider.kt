package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.CurrentIndicesSnapshot
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

/**
 * Gets updates from [RouteProgress], converts them into [CurrentIndicesSnapshot]
 * and provides said info.
 * Supports freezing and unfreezing of updates, see [freezeAndGet] and [unfreeze] for details.
 */
internal class CurrentIndicesSnapshotProvider :
    RouteProgressObserver, Function0<CurrentIndicesSnapshot> {

    private val defaultIndicesSnapshot = CurrentIndicesSnapshot()
    private var frozen = false
    private var indicesSnapshot = defaultIndicesSnapshot
    private var rememberedIndicesSnapshot: CurrentIndicesSnapshot? = null

    /**
     * Freezes updates and returns last saved value.
     * Updates will be frozen, meaning they will not affect [freezeAndGet] and [invoke] results,
     * but new info will be remembered and used as soon as state is unfrozen.
     */
    @Synchronized
    fun freezeAndGet(): CurrentIndicesSnapshot {
        frozen = true
        rememberedIndicesSnapshot = null
        return invoke()
    }

    /**
     * Unfreezes updates.
     */
    @Synchronized
    fun unfreeze() {
        frozen = false
        rememberedIndicesSnapshot?.let {
            indicesSnapshot = it
        }
        rememberedIndicesSnapshot = null
    }

    /**
     * Update indices. Will not update in frozen state,
     * but will remember the new info and use it as soon as state is unfrozen.
     */
    @Synchronized
    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        CurrentIndicesSnapshot(
            legIndex = routeProgress.currentLegProgress?.legIndex
                ?: defaultIndicesSnapshot.legIndex,
            routeGeometryIndex = routeProgress.currentRouteGeometryIndex,
            legGeometryIndex = routeProgress.currentLegProgress?.geometryIndex,
        ).also {
            if (frozen) {
                rememberedIndicesSnapshot = it
            } else {
                indicesSnapshot = it
            }
        }
    }

    /**
     * Returns last saved info.
     */
    @Synchronized
    override fun invoke(): CurrentIndicesSnapshot = indicesSnapshot
}
