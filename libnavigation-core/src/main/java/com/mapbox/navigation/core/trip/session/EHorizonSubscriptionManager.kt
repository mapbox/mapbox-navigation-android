package com.mapbox.navigation.core.trip.session

interface EHorizonSubscriptionManager {
    fun registerObserver(observer: EHorizonObserver)
    fun unregisterObserver(observer: EHorizonObserver)
    fun unregisterAllObservers()
    fun reset()
}
