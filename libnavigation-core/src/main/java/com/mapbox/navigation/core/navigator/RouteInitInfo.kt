package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject

internal data class RouteInitInfo(
    val roadObjects: List<UpcomingRoadObject>
)
