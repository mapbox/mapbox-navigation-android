@file:JvmName("MapboxRouteOptionsUtils")

package com.mapbox.navigation.base.extensions

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.internal.RouteUrl

fun RouteOptions.Builder.applyDefaultParams(): RouteOptions.Builder = also {
    baseUrl(RouteUrl.BASE_URL)
    user(RouteUrl.PROFILE_DEFAULT_USER)
    profile(RouteUrl.PROFILE_DRIVING)
    geometries(RouteUrl.GEOMETRY_POLYLINE6)
    requestUuid("")
}

@JvmOverloads
fun RouteOptions.Builder.coordinates(
    origin: Point,
    waypoints: List<Point?>? = null,
    destination: Point
): RouteOptions.Builder {
    val coordinates = mutableListOf<Point>().apply {
        add(origin)
        waypoints?.filterNotNull()?.forEach { add(it) }
        add(destination)
    }

    coordinates(coordinates)

    return this
}
