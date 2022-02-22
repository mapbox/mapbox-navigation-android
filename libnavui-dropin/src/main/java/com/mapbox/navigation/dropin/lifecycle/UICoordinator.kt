package com.mapbox.navigation.dropin.lifecycle

import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.UIBinder
import kotlinx.coroutines.flow.Flow

/**
 * Attach a UICoordinator to a [ViewGroup] of your choosing. When you implement this class
 * you will need to build a [Flow] with [Binder]. There can only be one view binder
 * attached at a time for the [ViewGroup].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
abstract class UICoordinator<T : ViewGroup>(
    protected val viewGroup: T
) : UIComponent() {

    protected var viewBinder: Binder<T>? = null
    protected var attachedObserver: MapboxNavigationObserver? = null

    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowViewBinders().observe { binder ->
            attachedObserver?.onDetached(mapboxNavigation)
            viewBinder?.also { unbind(it, viewGroup) }

            attachedObserver = bind(binder, viewGroup)
            viewBinder = binder
            attachedObserver?.onAttached(mapboxNavigation)
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        attachedObserver?.onDetached(mapboxNavigation)
        attachedObserver = null
        viewBinder?.also { unbind(it, viewGroup) }
        viewBinder = null
    }

    protected open fun bind(viewBinder: Binder<T>, viewGroup: T): MapboxNavigationObserver =
        viewBinder.bind(viewGroup)

    protected open fun unbind(viewBinder: Binder<T>, viewGroup: T): Unit =
        viewBinder.unbind(viewGroup)

    /**
     * Create your flowable [UIBinder]. This allows you to use a flowable state to
     * determine what is being shown in the [viewGroup].
     */
    abstract fun MapboxNavigation.flowViewBinders(): Flow<Binder<T>>
}
