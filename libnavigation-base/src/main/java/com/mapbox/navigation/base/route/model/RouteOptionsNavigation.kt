package com.mapbox.navigation.base.route.model

import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.Point

class RouteOptionsNavigation(
    val alternatives: Boolean?,
    val language: String?,
    val radiuses: String?,
    val coordinates: List<Point>?,
    val profile: String?,
    val bearings: String?,
    @SerializedName("continue_straight") val continueStraight: Boolean?,
    @SerializedName("roundabout_exits") val roundaboutExits: Boolean?,
    val geometries: String,
    val overview: String?,
    val steps: Boolean?,
    val annotations: String?,
    val exclude: String?,
    @SerializedName("voice_instructions") val voiceInstructions: Boolean?,
    @SerializedName("banner_instructions") val bannerInstructions: Boolean?,
    @SerializedName("voice_units") val voiceUnits: String?,
    @SerializedName("uuid") val requestUuid: String?,
    val approaches: String?,
    @SerializedName("waypoints") val waypointIndices: String?,
    @SerializedName("waypoint_names") val waypointNames: String?,
    @SerializedName("waypoint_targets") val waypointTargets: String?,
    val walkingOptions: WalkingOptionsNavigation?
)
