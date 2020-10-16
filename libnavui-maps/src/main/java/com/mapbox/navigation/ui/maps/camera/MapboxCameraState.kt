package com.mapbox.navigation.ui.maps.camera

/**
 * Defines set of states available for MapboxCamera
 */
enum class MapboxCameraState {
    IDLE,
    TRANSITION_TO_FOLLOWING,
    FOLLOWING,
    TRANSITION_TO_ROUTE_OVERVIEW,
    ROUTE_OVERVIEW
}
