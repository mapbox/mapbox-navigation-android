package com.mapbox.navigation.base.trip.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Describes the current phase of EV charging at a charging station along the route.
 */
@ExperimentalMapboxNavigationAPI
enum class ChargingState {

    /**
     * No charging required, or charging has stopped.
     */
    NOT_CHARGING,

    /**
     * Arrived at an EV charging station in guidance mode, waiting for charging to start.
     */
    AWAIT_CHARGING,

    /**
     * Charging is in progress.
     */
    CHARGING,

    /**
     * Charging started and the state of charge is already enough to continue,
     * or charging is unplanned by the current route.
     */
    EXTRA_CHARGING,
}
