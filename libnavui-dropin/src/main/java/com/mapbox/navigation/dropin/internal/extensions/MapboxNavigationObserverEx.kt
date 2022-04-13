@file:JvmName("MapboxNavigationObserverEx")

package com.mapbox.navigation.dropin.internal.extensions

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.binder.UIBinder

/**
 * When returning an observer from [UIBinder.bind], you can use this extension to return
 * a list of observers. This will attach one to many observers to your view binder.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun <T : MapboxNavigationObserver> navigationListOf(vararg elements: T) =
    object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            elements.forEach { it.onAttached(mapboxNavigation) }
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            elements.reversed().forEach { it.onDetached(mapboxNavigation) }
        }
    }
