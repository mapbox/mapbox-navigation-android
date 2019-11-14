package com.mapbox.navigation.route.offboard.extension

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteLegsNavigation

fun Location.toPoint(): Point = Point.fromLngLat(this.longitude, this.latitude)

fun DirectionsRoute.mapToRoute() = Route(
    routeIndex = routeIndex(),
    distance = distance(),
    duration = duration()?.toLong(),
    geometry = geometry(),
    weight = weight(),
    weightName = weightName(),
    voiceLanguage = voiceLanguage(),
    legs = legs()?.let { RouteLegsNavigation(it) }
)
