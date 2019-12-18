package com.mapbox.navigation.base.route.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.base.route.model.WalkingOptionsNavigation

/**
 * Class for specifying options for use with the walking profile.
 *
 * @param walkingSpeed Walking speed in meters per second. Must be between 0.14 and 6.94 meters per second.
 * Defaults to 1.42 meters per second
 *
 * @param walkwayBias A bias which determines whether the route should prefer or avoid the use of roads or paths
 * that are set aside for pedestrian-only use (walkways). The allowed range of values is from
 * -1 to 1, where -1 indicates indicates preference to avoid walkways, 1 indicates preference
 * to favor walkways, and 0 indicates no preference (the default).
 *
 * @param alleyBias A bias which determines whether the route should prefer or avoid the use of alleys. The
 * allowed range of values is from -1 to 1, where -1 indicates indicates preference to avoid
 * alleys, 1 indicates preference to favor alleys, and 0 indicates no preference (the default).
 */
class WalkingOptionsNavigationDto(
    @SerializedName("walking_speed") val walkingSpeed: Double?,
    @SerializedName("walkway_bias") val walkwayBias: Double?,
    @SerializedName("alley_bias") val alleyBias: Double?
)

fun WalkingOptionsNavigationDto.mapToModel() = WalkingOptionsNavigation(
    walkingSpeed = walkingSpeed,
    walkwayBias = walkwayBias,
    alleyBias = alleyBias
)
