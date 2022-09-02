package com.mapbox.navigation.core.trip.session

import androidx.annotation.UiThread

/**
 * Callback that provides the various session states that can happen within a navigation session
 *
 * @see NavigationSessionState
 */
@UiThread
fun interface NavigationSessionStateObserver {
    /**
     * Called whenever the navigation session state has changed
     *
     * @param navigationSession [NavigationSessionState]
     */
    fun onNavigationSessionStateChanged(navigationSession: NavigationSessionState)
}
