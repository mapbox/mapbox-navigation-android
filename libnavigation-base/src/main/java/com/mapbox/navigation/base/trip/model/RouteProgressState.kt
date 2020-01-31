package com.mapbox.navigation.base.trip.model

enum class RouteProgressState {
    ROUTE_INVALID,
    ROUTE_INITIALIZED,
    ROUTE_ARRIVED,
    LOCATION_TRACKING,
    LOCATION_STALE,
    ROUTE_UNCERTAIN
}
