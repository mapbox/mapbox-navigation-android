package com.mapbox.androidauto.car.routes

internal interface RoutesProvider {
    fun registerRoutesListener(listener: RoutesListener)
    fun unregisterRoutesListener(listener: RoutesListener)
}
