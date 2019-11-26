package com.mapbox.navigation.base.route.model

import com.google.gson.annotations.SerializedName

data class WalkingOptionsNavigation(
    @SerializedName("walking_speed") val walkingSpeed: Double?,
    @SerializedName("walkway_bias") val walkwayBias: Double?,
    @SerializedName("alley_bias") val alleyBias: Double?
)
