package com.mapbox.navigation.base.utils

import androidx.annotation.VisibleForTesting
import androidx.collection.LruCache
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsRouteFBWrapper
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.LegStepFBWrapper
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.utils.internal.logD

private const val LOG_TAG = "DecodeUtils"

// Memory limits expressed in number of decoded Points (each Point ≈ 56 B on heap).
// Using point-count bounds prevents unbounded growth for very long routes where a single
// geometry may contain tens of thousands of points.
internal const val COMPLETE_GEOMETRY_CACHE_MAX_POINTS = 75_000 // ~4 MB
internal const val STEPS_GEOMETRY_CACHE_MAX_POINTS = 150_000 // ~8 MB

/**
 * Provides utilities to decode geometries of [DirectionsRoute]s and [LegStep]s.
 * Results are cached up to [COMPLETE_GEOMETRY_CACHE_MAX_POINTS] /
 * [STEPS_GEOMETRY_CACHE_MAX_POINTS] decoded [Point]s respectively.
 */
object DecodeUtils {

    private val completeGeometryDecodeCache =
        object : LruCache<Pair<String, Int>, List<Point>>(COMPLETE_GEOMETRY_CACHE_MAX_POINTS) {
            override fun sizeOf(key: Pair<String, Int>, value: List<Point>): Int =
                value.size.coerceAtLeast(1)
        }

    private val stepsGeometryDecodeCache =
        object : LruCache<Pair<String, Int>, List<Point>>(STEPS_GEOMETRY_CACHE_MAX_POINTS) {
            override fun sizeOf(key: Pair<String, Int>, value: List<Point>): Int =
                value.size.coerceAtLeast(1)
        }
    private val cachedRoutes = RouteList(maxSize = 3)

    /**
     * Decodes geometry of a [DirectionsRoute] to a [LineString] and caches the result.
     * No decoding is performed if there is already a cached result available.
     *
     * @return decoded [LineString]
     */
    @JvmStatic
    fun DirectionsRoute.completeGeometryToLineString(): LineString {
        return LineString.fromLngLats(completeGeometryToPoints())
    }

    /**
     * Decodes geometry of a [DirectionsRoute] to a [List] of [Point]s and caches the result.
     * No decoding is performed if there is already a cached result available.
     *
     * @return decoded [List] of [Point]s
     */
    @JvmStatic
    fun DirectionsRoute.completeGeometryToPoints(): List<Point> {
        PerformanceTracker.trackPerformanceSync("DirectionsRoute.completeGeometryToPoints") {
            if (this is DirectionsRouteFBWrapper) {
                return geometryNumeric ?: emptyList()
            }
            return completeGeometryDecodeCache.getOrDecode(geometry(), precision())
        }
    }

    /**
     * Decodes geometries of all [LegStep]s in a [DirectionsRoute] to [LineString]s
     * and caches the results.
     * No decoding is performed for [LegStep]s, that already have a cached result available.
     *
     * @return decoded [LineString]s. The resulting collection is a nested list of:
     * ```
     * [ legs
     *   [ steps
     *     { step's geometry decoded to a line string }
     *   ]
     * ]
     * ```
     */
    @JvmStatic
    fun DirectionsRoute.stepsGeometryToLineString(): List<List<LineString>> {
        val precision = precision()
        cacheRoute(route = this, precision)
        return legs()?.map { leg ->
            leg.steps()?.map { step ->
                val points = stepsGeometryDecodeCache.getOrDecode(step.geometry(), precision)
                LineString.fromLngLats(points)
            }.orEmpty()
        }.orEmpty()
    }

    /**
     * Decodes geometries of all [LegStep]s in a [DirectionsRoute] to [List]s of [Point]s
     * and caches the results.
     * No decoding is performed for [LegStep]s, that already have a cached result available.
     *
     * @return decoded [List]s of [Point]s. The resulting collection is a nested list of:
     * ```
     * [ legs
     *   [ steps
     *     { step's geometry decoded to a list of points }
     *   ]
     * ]
     * ```
     */
    @JvmStatic
    fun DirectionsRoute.stepsGeometryToPoints(): List<List<List<Point>>> {
        PerformanceTracker.trackPerformanceSync("DirectionsRoute.stepsGeometryToPoints") {
            if (this is DirectionsRouteFBWrapper) {
                return legs()?.map { leg ->
                    leg?.steps()?.map { step ->
                        (step as LegStepFBWrapper).geometryNumeric ?: emptyList()
                    }.orEmpty()
                }.orEmpty()
            } else {
                val precision = precision()
                cacheRoute(route = this, precision)
                return legs()?.map { leg ->
                    leg.steps()?.map { step ->
                        stepsGeometryDecodeCache.getOrDecode(step.geometry(), precision)
                    }.orEmpty()
                }.orEmpty()
            }
        }
    }

    /**
     * Decodes geometry of a [LegStep] in a [DirectionsRoute] to a [LineString]
     * and caches the result.
     * No decoding is performed if there is already a cached result available.
     *
     * @param legStep of an encoded geometry
     * @return decoded [LineString]
     */
    @JvmStatic
    fun DirectionsRoute.stepGeometryToLineString(legStep: LegStep): LineString {
        return LineString.fromLngLats(stepGeometryToPoints(legStep))
    }

    /**
     * Decodes geometry of a [LegStep] in a [DirectionsRoute] to a [List] of [Point]s
     * and caches the result.
     * No decoding is performed if there is already a cached result available.
     *
     * @param legStep of an encoded geometry
     * @return decoded [List] of [Point]s
     */
    @JvmStatic
    fun DirectionsRoute.stepGeometryToPoints(legStep: LegStep): List<Point> {
        PerformanceTracker.trackPerformanceSync("DirectionsRoute.stepGeometryToPoints") {
            if (legStep is LegStepFBWrapper) {
                return legStep.geometryNumeric ?: emptyList()
            }
            val precision = precision()
            cacheRoute(route = this, precision)
            return stepsGeometryDecodeCache.getOrDecode(legStep.geometry(), precision)
        }
    }

    /**
     * Clears the caches that were filled by invoking
     * [stepGeometryToPoints], [completeGeometryToPoints], etc.
     */
    @JvmStatic
    internal fun clearCacheInternal() {
        synchronized(stepsGeometryDecodeCache) {
            cachedRoutes.clear()
            stepsGeometryDecodeCache.evictAll()
        }
        completeGeometryDecodeCache.evictAll()
    }

    @JvmStatic
    internal fun clearCacheInternalExceptFor(routes: List<DirectionsRoute>) {
        removeAllRoutesExcept(routes)
    }

    @VisibleForTesting
    internal fun completeGeometryCacheTotalPoints(): Int =
        completeGeometryDecodeCache.snapshot().values.sumOf { it.size }

    @VisibleForTesting
    internal fun stepsGeometryCacheTotalPoints(): Int =
        stepsGeometryDecodeCache.snapshot().values.sumOf { it.size }

    /**
     * todo Remove inline references to RouteOptions in favor of taking geometry type as an argument or expose the extensions on top of NavigationRoute instead.
     */
    private fun DirectionsRoute.precision(): Int {
        return if (routeOptions()?.geometries() == DirectionsCriteria.GEOMETRY_POLYLINE) {
            Constants.PRECISION_5
        } else {
            Constants.PRECISION_6
        }
    }

    private fun LruCache<Pair<String, Int>, List<Point>>.getOrDecode(
        geometry: String?,
        precision: Int,
    ): List<Point> {
        if (geometry == null) return emptyList()
        return synchronized(lock = this) {
            val key = geometry to precision
            get(key) ?: PerformanceTracker.trackPerformanceSync("DecodeUtils.decode-cache-miss") {
                PolylineUtils.decode(geometry, precision).also { put(key, it) }
            }
        }
    }

    private fun cacheRoute(route: DirectionsRoute, precision: Int) {
        PerformanceTracker.trackPerformanceSync("DecodeUtils.cacheRoute") {
            synchronized(stepsGeometryDecodeCache) {
                when (val result = cachedRoutes.add(route, precision)) {
                    is RouteList.AddResult.Reordered -> return@trackPerformanceSync
                    is RouteList.AddResult.Added -> result.evicted?.let { evicted ->
                        evicted.route.legs()?.forEach { leg ->
                            leg.steps()?.forEach { step ->
                                step.geometry()?.let { geometry ->
                                    stepsGeometryDecodeCache.remove(geometry to evicted.precision)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DirectionsRoute.routeIdForLogs() = "${requestUuid()}#${routeIndex()}"

    private fun removeAllRoutesExcept(routesToKeep: List<DirectionsRoute>) =
        PerformanceTracker.trackPerformanceSync("DecodeUtils.removeAllRoutesExcept") {
            synchronized(stepsGeometryDecodeCache) {
                logD(LOG_TAG) {
                    "Looking for routes to remove among cached:" +
                        " ${cachedRoutes.joinToString { it.route.routeIdForLogs() }}, " +
                        "while ${routesToKeep.joinToString(",") { it.routeIdForLogs() }} " +
                        "should be kept"
                }
                val toRemove = cachedRoutes.filter { cached ->
                    routesToKeep.none { cached.route.isSameRoute(it) }
                }
                toRemove.forEach { cachedRouteToRemove ->
                    logD(LOG_TAG) {
                        "Cleaning steps geometry caches for route:" +
                            " ${cachedRouteToRemove.route.routeIdForLogs()}"
                    }
                    cachedRouteToRemove.route.legs()?.forEach { leg ->
                        leg.steps()?.forEach { step ->
                            step.geometry()?.let { geometry ->
                                stepsGeometryDecodeCache.remove(
                                    geometry to cachedRouteToRemove.precision,
                                )
                            }
                        }
                    }
                    cachedRoutes.remove(cachedRouteToRemove)
                }
            }
        }

    private data class CachedRouteInfo(
        val route: DirectionsRoute,
        val precision: Int,
    )

    private class RouteList(private val maxSize: Int) {
        private val items = arrayListOf<CachedRouteInfo>()

        val size get() = items.size

        sealed class AddResult {
            object Reordered : AddResult()
            data class Added(val evicted: CachedRouteInfo?) : AddResult()
        }

        /**
         * Adds [route] to the list. If already present, moves it to the end (MRU) and returns
         * [AddResult.Reordered]. If new, adds it and returns [AddResult.Added] with the evicted
         * entry if the list exceeded [maxSize].
         */
        fun add(route: DirectionsRoute, precision: Int): AddResult {
            val existingIndex = items.indexOfFirst {
                it.route.isSameRoute(route) && it.precision == precision
            }
            if (existingIndex >= 0) {
                if (existingIndex != items.lastIndex) {
                    items.add(items.removeAt(existingIndex))
                }
                return AddResult.Reordered
            }
            items.add(CachedRouteInfo(route, precision))
            val evicted = if (items.size > maxSize) items.removeAt(0) else null
            return AddResult.Added(evicted)
        }

        fun remove(info: CachedRouteInfo) = items.remove(info)
        fun filter(predicate: (CachedRouteInfo) -> Boolean) = items.filter(predicate)
        fun joinToString(transform: (CachedRouteInfo) -> String) =
            items.joinToString(transform = transform, separator = ",")

        fun clear() = items.clear()
    }
}
