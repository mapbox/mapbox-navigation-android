package com.mapbox.navigation.dropin.binder

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.lifecycle.UICoordinator

/**
 * This interface works with the [UICoordinator]. Implementations of this class representation
 * are responsible for transitioning a view(s) into the [ViewGroup]. They are also responsible for
 * deciding what components should be part of the view.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface Binder<T> {

    /**
     * Triggered when this view binder instance is attached. The [viewGroup] returns a
     * [MapboxNavigationObserver] which gives this view a simple lifecycle.
     */
    fun bind(viewGroup: T): MapboxNavigationObserver
}

/**
 * This interface works with the [UICoordinator]. Implementations of this class representation
 * are responsible for transitioning a view(s) into the [ViewGroup]. They are also responsible for
 * deciding what components should be part of the view.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface UIBinder : Binder<ViewGroup> {
    companion object {
        /**
         * Defines the default [UIBinder] used by NavigationView
         */
        val USE_DEFAULT: UIBinder = UIBinder { NoOpMapboxNavigationObserver }
    }
}

@ExperimentalPreviewMapboxNavigationAPI
internal object NoOpMapboxNavigationObserver : MapboxNavigationObserver {
    override fun onAttached(mapboxNavigation: MapboxNavigation) = Unit
    override fun onDetached(mapboxNavigation: MapboxNavigation) = Unit
}
