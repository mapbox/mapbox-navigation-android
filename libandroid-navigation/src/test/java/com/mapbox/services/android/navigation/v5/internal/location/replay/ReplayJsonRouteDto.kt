package com.mapbox.services.android.navigation.v5.internal.location.replay

import com.google.gson.annotations.SerializedName

internal data class ReplayJsonRouteDto(
    var locations: List<ReplayLocationDto>,
    @SerializedName("route") var routeRequest: String
)
