package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.RouteUrl
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation

data class RouteOptionsNavigationDto(
    val baseUrl: String?,
    val user: String?,
    val profile: String?,
    val coordinates: List<DoubleArray>, // List<RoutePointNavigationDto>,
    val alternatives: Boolean?,
    val language: String?,
    val radiuses: String?,
    val bearings: String?,
    @SerializedName("continue_straight") val continueStraight: Boolean?,
    @SerializedName("roundabout_exits") val roundaboutExits: Boolean?,
    val geometries: String?,
    val overview: String?,
    val steps: Boolean?,
    val annotations: String?,
    @SerializedName("voice_instructions") val voiceInstructions: Boolean?,
    @SerializedName("banner_instructions") val bannerInstructions: Boolean?,
    @SerializedName("voice_units") val voiceUnits: String?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("uuid") val requestUuid: String?,
    val exclude: String?,
    val approaches: String?,
    @SerializedName("waypoints") val waypointIndices: String?,
    @SerializedName("waypoint_names") val waypointNames: String?,
    @SerializedName("waypoint_targets") val waypointTargets: String?,
    val walkingOptions: WalkingOptionsNavigationDto?
)

fun RouteOptionsNavigationDto.mapToModel() = RouteOptionsNavigation(
    baseUrl = baseUrl ?: RouteUrl.BASE_URL,
    user = user ?: RouteUrl.PROFILE_DEFAULT_USER,
    profile = profile ?: RouteUrl.PROFILE_DRIVING,
    origin = coordinates.retrieveOrigin().mapToModel(),
    waypoints = coordinates.retrieveWaypoints().map { it.mapToModel() },
    destination = coordinates.retrieveDestination().mapToModel(),
    alternatives = alternatives ?: RouteOptionsNavigation.ALTERNATIVES_DEFAULT_VALUE,
    language = language,
    radiuses = radiuses,
    bearings = bearings,
    continueStraight = continueStraight ?: RouteOptionsNavigation.CONTINUE_STRAIGHT_DEFAULT_VALUE,
    roundaboutExits = roundaboutExits ?: RouteOptionsNavigation.ROUNDABOUT_EXITS_DEFAULT_VALUE,
    geometries = geometries,
    overview = overview,
    steps = steps ?: RouteOptionsNavigation.STEPS_DEFAULT_VALUE,
    annotations = annotations,
    voiceInstructions = voiceInstructions
        ?: RouteOptionsNavigation.VOICE_INSTRUCTIONS_DEFAULT_VALUE,
    bannerInstructions = bannerInstructions
        ?: RouteOptionsNavigation.BANNER_INSTRUCTIONS_DEFAULT_VALUE,
    voiceUnits = voiceUnits,
    accessToken = accessToken,
    requestUuid = requestUuid,
    exclude = exclude,
    approaches = approaches,
    waypointIndices = waypointIndices,
    waypointNames = waypointNames,
    waypointTargets = waypointTargets,
    walkingOptions = walkingOptions?.mapToModel()
)

private fun List<DoubleArray>.retrieveOrigin(): RoutePointNavigationDto =
    this.first().let { RoutePointNavigationDto(Point.fromLngLat(it[0], it[1]), null, null) }

private fun List<DoubleArray>.retrieveWaypoints(): List<RoutePointNavigationDto> =
    this.drop(1).dropLast(1).map {
        RoutePointNavigationDto(
            Point.fromLngLat(it[0], it[1]),
            null,
            null
        )
    }

private fun List<DoubleArray>.retrieveDestination(): RoutePointNavigationDto =
    this.last().let { RoutePointNavigationDto(Point.fromLngLat(it[0], it[1]), null, null) }
