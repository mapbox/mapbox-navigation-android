package com.mapbox.navigation.base.utils

import android.util.Log
import android.util.LruCache
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.internal.utils.isSameRoute

/**
 * Provides utilities to decode geometries of [DirectionsRoute]s and [LegStep]s.
 * Results are cached for geometries of up to 3 [DirectionsRoute]s.
 */
object DecodeUtils {

    private val completeGeometryDecodeCache = LruCache<Pair<String, Int>, List<Point>>(3)

    private val stepsGeometryDecodeCache = LruCache<Pair<String, Int>, List<Point>>(1)
    private val cachedRoutes = arrayListOf<CachedRouteInfo>()

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
        return completeGeometryDecodeCache.getOrDecode(geometry(), precision())
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
        val precision = precision()
        cacheRoute(route = this, precision)
        return legs()?.map { leg ->
            leg.steps()?.map { step ->
                stepsGeometryDecodeCache.getOrDecode(step.geometry(), precision)
            }.orEmpty()
        }.orEmpty()
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
        val precision = precision()
        cacheRoute(route = this, precision)
        return stepsGeometryDecodeCache.getOrDecode(legStep.geometry(), precision)
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
            stepsGeometryDecodeCache.resize(1)
        }
        completeGeometryDecodeCache.evictAll()
    }

    @JvmStatic
    internal fun clearCacheInternalExceptFor(routes: List<DirectionsRoute>) {
        removeAllRoutesExcept(routes)
    }

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
            get(key) ?: PolylineUtils.decode(geometry, precision).also { put(key, it) }
        }
    }

    private fun cacheRoute(route: DirectionsRoute, precision: Int) {
        val stepCount = route.countSteps()
        synchronized(stepsGeometryDecodeCache) {
            cachedRoutes.removeAll { it.route.isSameRoute(route) && it.precision == precision }
            if (cachedRoutes.size > 2) {
                cachedRoutes.removeFirst()
            }
            cachedRoutes.add(CachedRouteInfo(route, precision, stepCount))
            stepsGeometryDecodeCache.resize(cachedRoutes.sumOf { it.stepCount }.coerceAtLeast(1))
        }
    }

    private fun DirectionsRoute.routeId() = "${requestUuid()}#${routeIndex()}"

    private fun removeAllRoutesExcept(routesToKeep: List<DirectionsRoute>) {
        synchronized(stepsGeometryDecodeCache) {
            Log.d("vadzim-test", "looking for a route to remove: ${cachedRoutes.joinToString(",") { it.route.routeId() }}")
            val routesToRemove = cachedRoutes.filter { cached ->
                routesToKeep.none { it.requestUuid() == cached.route.requestUuid() && it.routeIndex() == it.routeIndex()}
            }
            routesToRemove.forEach {
                Log.d("vadzim-test", "cleaning caches for route ${it.route.routeId()}")
                val routeToRemove = it.route
                it.route.legs()?.forEach {
                    it.steps()?.forEach { step ->
                        stepsGeometryDecodeCache.remove(step.geometry()!! to routeToRemove.precision())
                    }
                }
            }
            stepsGeometryDecodeCache.resize(cachedRoutes.sumOf { it.stepCount }.coerceAtLeast(1))
        }
    }

    private fun DirectionsRoute.countSteps(): Int {
        return legs()?.sumOf { leg ->
            leg.steps()?.count { it.geometry() != null } ?: 0
        } ?: 0
    }

    private class CachedRouteInfo(
        val route: DirectionsRoute,
        val precision: Int,
        val stepCount: Int,
    )
}
