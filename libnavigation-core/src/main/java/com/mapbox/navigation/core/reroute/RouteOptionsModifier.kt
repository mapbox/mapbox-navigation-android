package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions

internal interface RouteOptionsModifier {

    fun modify(options: RouteOptions): RouteOptions
}
