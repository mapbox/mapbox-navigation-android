package com.mapbox.navigation.core.reroute

/**
 * [RerouteState] observer
 */
fun interface RerouteStateObserver {

    /**
     * Invoked whenever re-route state has changed.
     */
    fun onRerouteStateChanged(rerouteState: RerouteState)
}
