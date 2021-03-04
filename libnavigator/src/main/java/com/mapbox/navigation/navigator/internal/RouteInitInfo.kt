package com.mapbox.navigation.navigator.internal

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject

data class RouteInitInfo(
    val roadObjects: List<RoadObject>
)
