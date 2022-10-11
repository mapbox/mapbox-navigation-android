package com.mapbox.navigation.ui.base.lifecycle

import android.view.ViewGroup
import androidx.annotation.UiThread
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

/**
 * This interface works with the [UICoordinator]. Implementations of this class representation
 * are responsible for transitioning a view(s) into the [ViewGroup]. They are also responsible for
 * deciding what components should be part of the view.
 */
interface Binder<T> {

    /**
     * Triggered when this view binder instance is attached. The [viewGroup] returns a
     * [MapboxNavigationObserver] which gives this view a simple lifecycle.
     */
    @UiThread
    fun bind(viewGroup: T): MapboxNavigationObserver
}

/**
 * This interface works with the [UICoordinator]. Implementations of this class representation
 * are responsible for transitioning a view(s) into the [ViewGroup]. They are also responsible for
 * deciding what components should be part of the view.
 */
fun interface UIBinder : Binder<ViewGroup> {
    companion object {
        /**
         * Defines the default [UIBinder] used by NavigationView
         */
        val USE_DEFAULT: UIBinder = UIBinder { NoOpMapboxNavigationObserver }
    }
}

internal object NoOpMapboxNavigationObserver : MapboxNavigationObserver {
    override fun onAttached(mapboxNavigation: MapboxNavigation) = Unit
    override fun onDetached(mapboxNavigation: MapboxNavigation) = Unit
}
