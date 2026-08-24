package com.mapbox.navigation.core.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.directions.route.DirectionsRouteGeometryUtils
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.directionsRouteContextDataRefOrNull
import com.mapbox.turf.TurfMeasurement

internal typealias NNNearestPointOnRoute = com.mapbox.directions.route.NearestPointOnRoute

/**
 * The nearest point found on a route geometry to a queried point.
 *
 * @property geometryIndex index of the geometry vertex at the start of the segment the nearest
 *  point lies on.
 * @property segmentFraction fraction in `[0, 1]` along that segment where the nearest point
 *  lies (`0` at the vertex [geometryIndex], `1` at the next vertex).
 * @property distanceAlongGeometryMeters distance from the search window start to the nearest
 *  point, measured along the route geometry, in meters.
 * @property distanceToRouteMeters perpendicular distance from the queried point to the nearest
 *  point on the route, in meters.
 */
@ExperimentalPreviewMapboxNavigationAPI
class NearestPointOnRoute internal constructor(
    val geometryIndex: Int,
    val segmentFraction: Double,
    val distanceAlongGeometryMeters: Double,
    val distanceToRouteMeters: Double,
)

@ExperimentalPreviewMapboxNavigationAPI
@JvmSynthetic
internal fun NNNearestPointOnRoute.mapToPlatform() = NearestPointOnRoute(
    geometryIndex = geometryIndex,
    segmentFraction = t,
    distanceAlongGeometryMeters = distanceAlongMeters,
    distanceToRouteMeters = distanceToRouteMeters,
)

/**
 * Normalize a bearing to be within the range of [0..360).
 *
 * Useful to normalize value from [TurfMeasurement.bearing].
 */
internal fun normalizeBearing(
    angle: Double,
): Double {
    return (angle + 360) % 360
}

/**
 * Find the nearest point on the route geometry to [point], searching only the window of
 * vertices [[startIndex], [endIndex]] of `geometry_numeric`. Pass `startIndex=0` and
 * `endIndex=last` to search the whole route. `distanceAlongMeters` in the result is
 * measured from [startIndex].
 *
 * Uses the most snapping-accurate ECEF (3D Cartesian) approach: a single pass with no
 * trigonometry in the search loop, accurate globally. Automatically uses precomputed
 * cumulative distances from the buffer when available, falling back to accumulation otherwise.
 *
 * Returns null if the route is not backed by a native buffer.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun nearestPointOnGeometryEcef(
    route: DirectionsRoute,
    point: Point,
    startIndex: Int,
    endIndex: Int,
): NearestPointOnRoute? {
    require(startIndex >= 0) { "startIndex must be >= 0" }
    require(endIndex >= 0) { "endIndex must be >= 0" }
    require(endIndex > startIndex) { "endIndex must be > startIndex" }
    val buf = route.directionsRouteContextDataRefOrNull() ?: return null
    return DirectionsRouteGeometryUtils.nearestPointOnGeometryEcef(buf, startIndex, endIndex, point)
        ?.mapToPlatform()
}

/**
 * Find the nearest point on the route geometry to [point], searching only the window of
 * vertices [[startIndex], [endIndex]] of `geometry_numeric`. Pass `startIndex=0` and
 * `endIndex=last` to search the whole route. `distanceAlongMeters` in the result is
 * measured from [startIndex].
 *
 * Uses the Fast Cheap-ruler approach: a single cosine at setup and zero trigonometry in the
 * search loop — faster than [nearestPointOnGeometryEcef] when the search window spans less
 * than ~1° of latitude. The `distanceToRouteMeters` accuracy is sub-centimeter at
 * route-segment scale. Automatically uses precomputed cumulative distances from the buffer
 * when available, falling back to accumulation otherwise (quality will be worse on long routes).
 *
 * Returns null if the route is not backed by a native buffer.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun nearestPointOnGeometryCheapRuler(
    route: DirectionsRoute,
    point: Point,
    startIndex: Int,
    endIndex: Int,
): NearestPointOnRoute? {
    require(startIndex >= 0) { "startIndex must be >= 0" }
    require(endIndex >= 0) { "endIndex must be >= 0" }
    require(endIndex > startIndex) { "endIndex must be > startIndex" }
    val buf = route.directionsRouteContextDataRefOrNull() ?: return null
    return DirectionsRouteGeometryUtils.nearestPointOnGeometryCheapRuler(
        buf,
        startIndex,
        endIndex,
        point,
    )?.mapToPlatform()
}

/**
 * Find the nearest point on the route geometry to [point], searching only the window of
 * vertices [[startIndex], [endIndex]] of `geometry_numeric`. Pass `startIndex=0` and
 * `endIndex=last` to search the whole route. `distanceAlongMeters` in the result is
 * measured from [startIndex].
 *
 * Faithful port of the Mapbox Java Turf `nearestPointOnLine` algorithm: per segment it
 * evaluates the distance to both endpoints and to the perpendicular foot (great-circle
 * bearing + destination + segment intersection) and keeps the closest, using Turf's R=6373 km
 * spherical earth model, so results match the Java Turf implementation.
 *
 * Returns null if the route is not backed by a native buffer.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun nearestPointOnGeometryTurf(
    route: DirectionsRoute,
    point: Point,
    startIndex: Int,
    endIndex: Int,
): NearestPointOnRoute? {
    require(startIndex >= 0) { "startIndex must be >= 0" }
    require(endIndex >= 0) { "endIndex must be >= 0" }
    require(endIndex > startIndex) { "endIndex must be > startIndex" }
    val buf = route.directionsRouteContextDataRefOrNull() ?: return null
    return DirectionsRouteGeometryUtils.nearestPointOnGeometryTurf(buf, startIndex, endIndex, point)
        ?.mapToPlatform()
}
