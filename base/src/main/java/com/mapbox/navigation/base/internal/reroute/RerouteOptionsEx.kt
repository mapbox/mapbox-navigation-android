package com.mapbox.navigation.base.internal.reroute

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.options.RerouteOptions.Builder

/**
 * Delay in seconds before repeating reroute after off-route event if reroute did not happen
 *
 * Default value is -1 (turn-off)
 *
 * @return this [Builder]
 */
@ExperimentalPreviewMapboxNavigationAPI
fun RerouteOptions.Builder.setRepeatRerouteAfterOffRouteDelaySeconds(delaySeconds: Int): Builder {
    check(delaySeconds >= -1) {
        "repeatRerouteAfterOffRouteDelaySeconds must be higher or equal -1"
    }
    this.repeatRerouteAfterOffRouteDelaySeconds(delaySeconds)
    return this
}

@ExperimentalPreviewMapboxNavigationAPI
fun RerouteOptions.getRepeatRerouteAfterOffRouteDelaySeconds() =
    repeatRerouteAfterOffRouteDelaySeconds
