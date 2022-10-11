package com.mapbox.navigation.dropin.navigationview

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * There is a single ViewModel for the navigation view. Use this class to store state that should
 * survive configuration changes.
 */
internal class NavigationViewModel : ViewModel() {

    /**
     * LifecycleOwner available for attaching events to a lifecycle that will survive configuration
     * changes. This is only available to the [NavigationViewModel] for now. We can consider
     * exposing the LifecycleOwner to downstream components, but we do not have a use for it yet.
     */
    private val viewModelLifecycleOwner = NavigationViewModelLifecycleOwner()

    init {
        MapboxNavigationApp.attach(viewModelLifecycleOwner)
    }

    override fun onCleared() {
        viewModelLifecycleOwner.destroy()
    }
}

/**
 * The [MapboxNavigationApp] needs a scope that can survive configuration changes.
 * Everything inside the [NavigationViewModel] will survive the orientation change, we can
 * assume that the [MapboxNavigationApp] is [Lifecycle.State.CREATED] between init and onCleared.
 *
 * The [Lifecycle.State.STARTED] and [Lifecycle.State.RESUMED] states are represented by the
 * hosting view [NavigationView].
 */
private class NavigationViewModelLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
        .apply { currentState = Lifecycle.State.CREATED }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
