package com.mapbox.navigation.core.internal.extensions

import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

fun MapboxNavigationProvider.registerObserver(observer: MapboxNavigationObserver) {
    registerObserver(observer)
}

fun MapboxNavigationProvider.unregisterObserver(observer: MapboxNavigationObserver) {
    unregisterObserver(observer)
}
