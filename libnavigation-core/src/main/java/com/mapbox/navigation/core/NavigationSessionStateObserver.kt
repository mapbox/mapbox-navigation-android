package com.mapbox.navigation.core

internal interface NavigationSessionStateObserver {
    fun onNavigationSessionStateChanged(navigationSession: NavigationSession.State)
}
