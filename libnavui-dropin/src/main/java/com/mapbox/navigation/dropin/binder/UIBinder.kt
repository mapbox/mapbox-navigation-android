package com.mapbox.navigation.dropin.binder

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

/**
 * This interface works with the [UICoordinator]. Implementations of this class represent
 * are responsible for transitioning a view(s) into the [ViewGroup]. They are also responsible for
 * deciding what components should be part of the view.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
interface Binder<T> {

    /**
     * Triggered when this view binder instance is attached. The [viewGroup] returns a
     * [MapboxNavigationObserver] which gives this view a simple lifecycle.
     */
    fun bind(value: T): MapboxNavigationObserver
}

/**
 * This interface works with the [UICoordinator]. Implementations of this class represent
 * are responsible for transitioning a view(s) into the [ViewGroup]. They are also responsible for
 * deciding what components should be part of the view.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
interface UIBinder : Binder<ViewGroup>

/**
 * When returning an observer from [UIBinder.bind], you can use this extension to return
 * a list of observers. This will attach one to many observers to your view binder.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal fun <T : MapboxNavigationObserver> navigationListOf(vararg elements: T) =
    object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            elements.forEach { it.onAttached(mapboxNavigation) }
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            elements.reversed().forEach { it.onDetached(mapboxNavigation) }
        }
    }
