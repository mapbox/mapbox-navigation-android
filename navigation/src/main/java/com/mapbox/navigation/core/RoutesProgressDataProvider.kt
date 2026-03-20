package com.mapbox.navigation.core

import androidx.annotation.MainThread
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.RoutesProgressData
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
        val legProgress = routeProgress.currentLegProgress
        val primary = RouteProgressData(
            legIndex = legProgress?.legIndex ?: defaultRouteProgressData.legIndex,
            routeGeometryIndex = routeProgress.currentRouteGeometryIndex,
            legGeometryIndex = legProgress?.geometryIndex,
        ).clampGeometryIndices(routeProgress)
        val alternatives = routeProgress.internalAlternativeRouteIndices().entries.associate {
            it.key to RouteProgressData(
                it.value.legIndex,
                it.value.routeGeometryIndex,
                it.value.legGeometryIndex,
            )
        }
        routesProgressData = RoutesProgressData(primary, alternatives).also {
            continuation?.resume(it)
            continuation = null
        }
    }

    /**
     * Navigator may advance geometry index beyond current leg's geometry
     * in COMPLETE state while still on the same leg — clamp it back.
     */
    private fun RouteProgressData.clampGeometryIndices(
        routeProgress: RouteProgress,
    ): RouteProgressData {
        if (routeProgress.currentState != RouteProgressState.COMPLETE) return this
        val legGeometryIndex = legGeometryIndex ?: return this
        val legAnnotationsCount =
            routeProgress.currentLegProgress?.routeLeg?.annotationsCount() ?: return this
        if (legGeometryIndex < legAnnotationsCount) return this

        val overshot = legGeometryIndex - (legAnnotationsCount - 1)
        return copy(
            legGeometryIndex = legAnnotationsCount - 1,
            routeGeometryIndex = routeGeometryIndex - overshot,
        )
    }

    /**
     * The number of annotations for the route leg.
     * If there are any unrecognized annotations, they will be checked as well.
     */
    private fun RouteLeg.annotationsCount(): Int? {
        val recognized = annotation()?.run {
            sequence {
                yield(duration()?.size)
                yield(speed()?.size)
                yield(distance()?.size)
                yield(congestion()?.size)
                yield(congestionNumeric()?.size)
                yield(maxspeed()?.size)
                yield(freeflowSpeed()?.size)
                yield(currentSpeed()?.size)
                yield(trafficTendency()?.size)
            }
        } ?: emptySequence()

        val unrecognized = sequence {
            unrecognizedJsonProperties?.forEach {
                if (it.value.isJsonArray) {
                    yield(it.value.asJsonArray.size())
                }
            }
        }

        return (recognized + unrecognized).filterNotNull().firstOrNull()
    }
}
