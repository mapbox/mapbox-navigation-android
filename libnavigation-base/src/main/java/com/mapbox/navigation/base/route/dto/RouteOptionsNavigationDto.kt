package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.base.route.RouteUrl
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.route.model.RoutePointNavigation

internal data class RouteOptionsNavigationDto(
    val baseUrl: String?,
    val user: String?,
    val profile: String?,
    val coordinates: List<RoutePointNavigationDto>,
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
    val accessToken: String?,
    @SerializedName("uuid") val requestUuid: String?,
    val exclude: String?,
    val approaches: String?,
    @SerializedName("waypoints") val waypointIndices: String?,
    @SerializedName("waypoint_names") val waypointNames: String?,
    @SerializedName("waypoint_targets") val waypointTargets: String?,
    val walkingOptions: WalkingOptionsNavigationDto?
)

internal fun RouteOptionsNavigationDto.mapToModel() = RouteOptionsNavigation(
    baseUrl = baseUrl ?: RouteUrl.BASE_URL,
    user = user ?: RouteUrl.PROFILE_DEFAULT_USER,
    profile = profile ?: RouteUrl.PROFILE_DRIVING,
    origin = coordinates.map(RoutePointNavigationDto::mapToModel).retriveOrigin(),
    waypoints = coordinates.map(RoutePointNavigationDto::mapToModel).retriveWaypoints(),
    destination = coordinates.map(RoutePointNavigationDto::mapToModel).retriveDestination(),
    alternatives = alternatives ?: false,
    language = language,
    radiuses = radiuses,
    bearings = bearings,
    continueStraight = continueStraight ?: false,
    roundaboutExits = roundaboutExits,
    geometries = geometries,
    overview = overview,
    steps = steps,
    annotations = annotations,
    voiceInstructions = voiceInstructions,
    bannerInstructions = bannerInstructions,
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

private fun List<RoutePointNavigation>.retriveOrigin(): RoutePointNavigation = this.first()

private fun List<RoutePointNavigation>.retriveWaypoints(): List<RoutePointNavigation> = this.drop(1).dropLast(1)

private fun List<RoutePointNavigation>.retriveDestination(): RoutePointNavigation = this.first()
