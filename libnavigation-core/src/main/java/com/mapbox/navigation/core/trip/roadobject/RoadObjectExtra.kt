package com.mapbox.navigation.core.trip.roadobject

/**
 * Utility class that maps leg index of a route to list of road objects.
 *
 * @param routeId id of the route
 * @param legIndexToRoadObject map of route leg index to list of road objects.
 */
class RoadObjectExtra<T> internal constructor(
    val routeId: String,
    val legIndexToRoadObject: Map<Int, List<T>>
)
