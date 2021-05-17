package com.mapbox.navigation.core

internal fun interface NavigationSessionStateObserver {
    fun onNavigationSessionStateChanged(navigationSession: NavigationSession.State)
}
