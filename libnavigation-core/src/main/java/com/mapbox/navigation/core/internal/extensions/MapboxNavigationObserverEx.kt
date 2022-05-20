@file:JvmName("MapboxNavigationObserverEx")

package com.mapbox.navigation.core.internal.extensions

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

/**
 * When returning an observer, you can use this extension to return a list of observers. This will
 * attach one to many observers to your view binder.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun <T : MapboxNavigationObserver> navigationListOf(vararg elements: T) =
    object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            elements.forEach { it.onAttached(mapboxNavigation) }
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            elements.reversed().forEach { it.onDetached(mapboxNavigation) }
        }
    }
