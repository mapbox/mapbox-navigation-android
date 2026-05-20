package com.mapbox.navigation.instrumentation_tests.utils

import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfJoins
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.milliseconds

/**
 * Test-side helper that polls [TileStore.computeCoveredArea] to estimate how much of a
 * [NavigationRoute] is currently covered by tiles in the predictive-cache groups for the
 * given [descriptors].
 *
 * Bound to a single [tileStore] + [descriptors] pair; reuse across multiple routes during
 * a test by calling [awaitCoversRoute] for each one.
 *
 * Important caveats:
 *  - The covered-area polygon includes ALL cached tiles for those descriptors (ambient
 *    circle, route-buffer ribbon, anything left over from earlier in the run). With a
 *    fresh [TileStore.create()] this is a clean signal; with a shared store it can read
 *    high before the route-aware worker has actually drained.
 *  - Tiles are ~2.4 km wide at z=14, so a sample point near a tile edge reads as covered
 *    while the neighbouring tile is still downloading. Useful for "route is drivable
 *    offline" — not for "every byte the worker wanted has landed."
 */
class PredictiveCacheMonitor(
    private val tileStore: TileStore,
    private val descriptors: List<TilesetDescriptor>,
) {

    /**
     * Polls until [requiredCoverageFraction] of the route's sample points fall inside the
     * tile-store covered-area polygon, or until [timeoutMs] elapses.
     *
     * @return the last observed coverage fraction in `[0.0, 1.0]`.
     * @throws kotlinx.coroutines.TimeoutCancellationException if coverage never reached
     *   the required fraction in time.
     */
    suspend fun awaitCoversRoute(
        route: NavigationRoute,
        requiredCoverageFraction: Double = 1.0,
        pollIntervalMs: Long = 500L,
        timeoutMs: Long = 30_000L,
        sampleStepMeters: Double = 10.0,
        tag: String = "",
    ): Double {
        val samples = route.samplePoints(sampleStepMeters)
        check(samples.isNotEmpty()) { "[$tag] route geometry produced no sample points" }
        var lastFraction = 0.0
        try {
            return withTimeout(timeoutMs.milliseconds) {
                while (true) {
                    val coverage = tileStore.computeCoveredAreaSuspend(descriptors)
                    val covered = samples.count { coverage.containsPoint(it) }
                    lastFraction = covered.toDouble() / samples.size
                    Log.i(
                        LOG_TAG,
                        "[$tag] $covered/${samples.size} " +
                            "(${"%.1f".format(lastFraction * 100)}%) " +
                            "covered for route ${route.id}",
                    )
                    if (lastFraction >= requiredCoverageFraction) return@withTimeout lastFraction
                    delay(pollIntervalMs.milliseconds)
                }
                @Suppress("UNREACHABLE_CODE")
                lastFraction
            }
        } catch (t: Throwable) {
            Log.w(
                LOG_TAG,
                "[$tag] timed out at ${"%.1f".format(lastFraction * 100)}% " +
                    "for route ${route.id}",
            )
            throw t
        }
    }

    private companion object {
        const val LOG_TAG = "PredictiveCacheCov"
    }
}

private suspend fun TileStore.computeCoveredAreaSuspend(
    descriptors: List<TilesetDescriptor>,
): Geometry? = suspendCoroutine { cont ->
    computeCoveredArea(descriptors) { result ->
        cont.resume(if (result.isValue) result.value else null)
    }
}

private fun NavigationRoute.samplePoints(stepMeters: Double): List<Point> {
    val encoded = directionsRoute.geometry() ?: return emptyList()
    val precision = if (routeOptions.geometries() == DirectionsCriteria.GEOMETRY_POLYLINE6) 6 else 5
    val line = LineString.fromPolyline(encoded, precision)
    val totalKm = TurfMeasurement.length(line, TurfConstants.UNIT_KILOMETERS)
    val stepKm = stepMeters / 1000.0
    val nSteps = (totalKm / stepKm).toInt().coerceAtLeast(2)
    return (0..nSteps).map { i ->
        TurfMeasurement.along(line, stepKm * i, TurfConstants.UNIT_KILOMETERS)
    }
}

private fun Geometry?.containsPoint(point: Point): Boolean = when (this) {
    null -> false
    is Polygon -> TurfJoins.inside(point, this)
    is MultiPolygon -> coordinates().any { rings ->
        TurfJoins.inside(point, Polygon.fromLngLats(rings))
    }
    else -> false
}
