package com.mapbox.services.android.navigation.v5.location.replay

import com.google.gson.annotations.SerializedName

private class ReplayJsonRouteDto {

    var locations: List<ReplayLocationDto>? = null
    @SerializedName("route")
    var routeRequest: String? = null
}
