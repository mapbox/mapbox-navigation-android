package com.mapbox.navigation.base.trip.model

/**
 * Contains the various progress states that can occur while navigating.
 */
enum class RouteProgressState {

    /**
     * We have initialized route, but haven't started route tracking yet: all [RouteProgress] fields are valid.
     */
    INITIALIZED,

    /**
     * We are on the route: all [RouteProgress] fields are valid.
     */
    TRACKING,

    /**
     * We've approached to the end of route's leg: all [RouteProgress] fields are valid.
     */
    COMPLETE,

    /**
     * We've gone off route: all [RouteProgress] fields are invalid.
     */
    OFF_ROUTE,

    /**
     * We are probably about to go off-route: all [RouteProgress] fields are valid.
     */
    UNCERTAIN,
}
