@file:JvmName("ForwardMapboxNavigation")

package com.mapbox.navigation.core.lifecycle

import com.mapbox.navigation.core.MapboxNavigation

/**
 * This extension removes boilerplate from a class that needs to use [MapboxNavigation], and it
 * makes your class appear as if it implements [MapboxNavigationObserver] without exposing the
 * functions.
 *
 * ``` kotlin
 * val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)
 * private fun onAttached(mapboxNavigation: MapboxNavigation)
 * private fun onDetached(mapboxNavigation: MapboxNavigation)
 * ```
 */
fun forwardMapboxNavigation(
    attach: (MapboxNavigation) -> Unit,
    detach: (MapboxNavigation) -> Unit,
) = object : MapboxNavigationObserver {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        attach(mapboxNavigation)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        detach(mapboxNavigation)
    }
}
