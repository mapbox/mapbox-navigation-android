package com.mapbox.navigation.route.offboard.router

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.RouteUrl
import java.util.*


private val EVENT_LISTENER = NavigationRouteEventListener()
private const val SEMICOLON = ";"
private const val COMMA = ","

fun MapboxDirections.Builder.routeOptions(options: RouteOptions): MapboxDirections.Builder {
    baseUrl(options.baseUrl())
    user(options.user())
    profile(options.profile())

    options.coordinates().forEachIndexed { index, point ->
        when (index) {
            0 -> origin(point)
            options.coordinates().size - 1 -> destination(point)
            else -> addWaypoint(point)
        }
    }

    alternatives(options.alternatives() ?: false)

    options.language()?.let {
        language(Locale(it))
    }
    options.radiuses()?.let { radiuses ->
        if (radiuses.isNotEmpty()) {
            radiuses.convertToListOfDoubles(SEMICOLON[0])?.toDoubleArray()?.let { result ->
                radiuses(*result)
            }
        }
    }

    options.bearings()?.let { bearings ->
        if (bearings.isNotEmpty()) {
            bearings.convertToListOfPairsOfDoubles(SEMICOLON[0], COMMA[0])
                ?.forEach { pair ->
                    addBearing(pair.first, pair.second)
                }
        }
    }

    continueStraight(options.continueStraight() ?: false)
    roundaboutExits(options.roundaboutExits() ?: false)

    options.geometries()?.let {
        geometries(options.geometries())
    }

    options.overview()?.let {
        overview(it)
    }

    steps(options.steps() ?: true)

    options.annotations()?.let {
        annotations(it)
    }

    voiceInstructions(options.voiceInstructions() ?: true)
    bannerInstructions(options.bannerInstructions() ?: true)

    options.voiceUnits()?.let {
        voiceUnits(it)
    }

    accessToken(options.accessToken())

    options.requestUuid().let {
        // TODO Check if needed as it is only set at response time
    }

    options.exclude()?.let {
        exclude(it)
    }

    options.approaches()?.let { approaches ->
        if (approaches.isNotEmpty()) {
            val result =
                approaches.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            addApproaches(*result)
        }
    }

    options.waypointIndices()?.let { waypointIndices ->
        if (waypointIndices.isNotEmpty()) {
            val splitWaypointIndices = parseWaypointIndices(waypointIndices)
            addWaypointIndices(*splitWaypointIndices)
        }
    }

    options.waypointNames()?.let { waypointNames ->
        if (waypointNames.isNotEmpty()) {
            val names =
                waypointNames.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            addWaypointNames(*names)
        }
    }

    options.waypointTargets()?.let { waypointTargets ->
        if (waypointTargets.isNotEmpty()) {
            val splitWaypointTargets = parseWaypointTargets(waypointTargets)
            addWaypointTargets(*splitWaypointTargets)
        }
    }

    options.walkingOptions()?.let {
        walkingOptions(it)
    }

    eventListener(EVENT_LISTENER)

    return this
}

private fun String.convertToListOfDoubles(separator: Char = ';'): List<Double>? =
    try {
        this.split(separator).map { token ->
            token.toDouble()
        }
    } catch (e: Exception) {
        null
    }

private fun String.convertToListOfPairsOfDoubles(
    firstSeparator: Char = ';',
    secondSeparator: Char = ','
): List<Pair<Double, Double>>? =
    try {
        val pairs = split(firstSeparator)
        val result = mutableListOf<Pair<Double, Double>>()
        pairs.forEach { pair ->
            val parts = pair.split(secondSeparator)
            result.add(Pair(parts[0].toDouble(), parts[1].toDouble()))
        }
        result.toList()
    } catch (e: Exception) {
        null
    }


private fun parseWaypointIndices(waypointIndices: String): Array<Int> {
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

private fun parseWaypointTargets(waypointTargets: String): Array<Point?> {
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
            val longitude = java.lang.Double.valueOf(point[0])
            val latitude = java.lang.Double.valueOf(point[0])
            waypoints[index++] = Point.fromLngLat(longitude, latitude)
        }
    }
    return waypoints
}

fun RouteOptions.Builder.applyDefaultParams() : RouteOptions.Builder = also {
    baseUrl(RouteUrl.BASE_URL)
    user(RouteUrl.PROFILE_DEFAULT_USER)
    profile(RouteUrl.PROFILE_DRIVING)
    geometries(RouteUrl.GEOMETRY_POLYLINE6)
    requestUuid("")
}