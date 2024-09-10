package com.mapbox.navigation.ui.base.lifecycle

import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Attach a UICoordinator to a [ViewGroup] of your choosing. When you implement this class
 * you will need to build a [Flow] with [Binder]. There can only be one view binder
 * attached at a time for the [ViewGroup].
 *
 * @property coroutineScope Defines a scope for new coroutine
 */
abstract class UICoordinator<T : ViewGroup>(
    private val viewGroup: T,
) : MapboxNavigationObserver {

    private var attachedObserver: MapboxNavigationObserver? = null
    lateinit var coroutineScope: CoroutineScope

    /**
     * Signals that the [mapboxNavigation] instance is attached and ready for use.
     *
     * @param mapboxNavigation
     */
    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        coroutineScope = MainScope()

        coroutineScope.launch {
            mapboxNavigation.flowViewBinders().collect { viewBinder ->
                attachedObserver?.onDetached(mapboxNavigation)
                attachedObserver = viewBinder.bind(viewGroup)
                attachedObserver?.onAttached(mapboxNavigation)
            }
        }
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     *
     * @param mapboxNavigation
     */
    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        coroutineScope.cancel()
        attachedObserver?.onDetached(mapboxNavigation)
        attachedObserver = null
    }

    /**
     * Create your flowable [Binder]. This allows you to use a flowable state to
     * determine what is being shown in the [viewGroup].
     */
    abstract fun MapboxNavigation.flowViewBinders(): Flow<Binder<T>>
}
