@file:JvmName("MapboxRouteOptionsUtils")

package com.mapbox.navigation.base.extensions

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.RouteUrl

const val SEMICOLON = ";"
const val COMMA = ","

fun RouteOptions.checkFields() {
    check(this.coordinates().size >= 2) { "At least 2 coordinates should be provided." }
}

fun String.convertToListOfDoubles(separator: Char = ';'): List<Double>? =
    try {
        this.split(separator).map { token ->
            token.toDouble()
        }
    } catch (e: Exception) {
        null
    }

fun String.convertToListOfPairsOfDoubles(
    firstSeparator: Char = ';',
    secondSeparator: Char = ','
): List<Pair<Double, Double>?>? =
    try {
        val pairs = split(firstSeparator)
        val result = mutableListOf<Pair<Double, Double>?>()
        pairs.forEach { pair ->
            val parts = pair.split(secondSeparator)
            if (parts.size == 1) {
                result.add(null)
            } else {
                result.add(Pair(parts[0].toDouble(), parts[1].toDouble()))
            }
        }
        result.toList()
    } catch (e: Exception) {
        null
    }

fun parseWaypointIndices(waypointIndices: String): Array<Int> {
    val splitWaypointIndices =
        waypointIndices.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    val indices = Array(splitWaypointIndices.size) { 0 }
    for ((index, waypointIndex) in splitWaypointIndices.withIndex()) {
        val parsedIndex = Integer.valueOf(waypointIndex)
        indices[index] = parsedIndex
    }
    return indices
}

fun parseWaypointTargets(waypointTargets: String): Array<Point?> {
    val splitWaypointTargets =
        waypointTargets.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    val waypoints = arrayOfNulls<Point>(splitWaypointTargets.size)
    var index = 0
    for (waypointTarget in splitWaypointTargets) {
        val point = waypointTarget.split(COMMA.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (waypointTarget.isEmpty()) {
            waypoints[index++] = null
        } else {
            val longitude = point[0].toDouble()
            val latitude = point[1].toDouble()
            waypoints[index++] = Point.fromLngLat(longitude, latitude)
        }
    }
    return waypoints
}

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

fun RouteOptions.Builder.bearings(vararg pairs: Pair<Double, Double>?): RouteOptions.Builder {
    val builder = StringBuilder()
    pairs.forEachIndexed { index, pair ->
        if (pair != null) {
            builder.append("${pair.first},${pair.second}")
        }

        if (index != pairs.size - 1) {
            builder.append(";")
        }
    }

    bearings(builder.toString())

    return this
}
