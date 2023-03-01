package com.mapbox.navigation.core.routerefresh

internal interface RouteRefreshProgressObserver {

    fun onStarted()

    fun onSuccess()

    fun onFailure(message: String?)

    fun onClearedExpired()

    fun onCancel()
}
