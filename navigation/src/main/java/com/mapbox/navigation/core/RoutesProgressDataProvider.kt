package com.mapbox.navigation.core

import androidx.annotation.MainThread
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.route.NavigationRoute
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

    // route id to per-leg annotations count, extracted eagerly to avoid holding full routes
    private var altAnnotationsCountsById = emptyMap<String, List<Int?>>()

    /**
     * Returns either last saved value (if has one) or waits for the next update.
     */
    suspend fun getRouteRefreshRequestDataOrWait(): RoutesProgressData {
        return (routesProgressData ?: suspendCancellableCoroutine { continuation = it })
    }

    fun onNewRoutes(routes: List<NavigationRoute> = emptyList()) {
        routesProgressData = null
        altAnnotationsCountsById = routes.associate { route ->
            route.id to route.directionsRoute.legs().orEmpty().map { it.annotationsCount() }
        }
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val isComplete = routeProgress.currentState == RouteProgressState.COMPLETE
        val legProgress = routeProgress.currentLegProgress
        val primary = RouteProgressData(
            legIndex = legProgress?.legIndex ?: defaultRouteProgressData.legIndex,
            routeGeometryIndex = routeProgress.currentRouteGeometryIndex,
            legGeometryIndex = legProgress?.geometryIndex,
        ).clampGeometryIndices(isComplete) { legProgress?.routeLeg?.annotationsCount() }
        val alternatives = routeProgress.internalAlternativeRouteIndices().entries.associate {
            it.key to RouteProgressData(
                it.value.legIndex,
                it.value.routeGeometryIndex,
                it.value.legGeometryIndex,
            ).clampGeometryIndices(isComplete) {
                altAnnotationsCountsById[it.key]?.getOrNull(it.value.legIndex)
            }
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
        isComplete: Boolean,
        legAnnotationsCount: () -> Int?,
    ): RouteProgressData {
        if (!isComplete) return this
        val legGeometryIndex = legGeometryIndex ?: return this
        val annotationsCount = legAnnotationsCount() ?: return this
        if (annotationsCount !in 1..legGeometryIndex) return this

        val overshot = legGeometryIndex - (annotationsCount - 1)
        return copy(
            legGeometryIndex = annotationsCount - 1,
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
