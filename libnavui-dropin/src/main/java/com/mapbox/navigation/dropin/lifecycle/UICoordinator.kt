package com.mapbox.navigation.dropin.lifecycle

import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.binder.UIBinder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Attach a UICoordinator to a [ViewGroup] of your choosing. When you implement this class
 * you will need to build a [Flow] with [Binder]. There can only be one view binder
 * attached at a time for the [ViewGroup].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
abstract class UICoordinator(
    private val viewGroup: ViewGroup
) : UIComponent() {

    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        var attachedObserver: MapboxNavigationObserver? = null

        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            mapboxNavigation.flowViewBinders().collect { viewBinder ->
                attachedObserver?.onDetached(mapboxNavigation)
                attachedObserver = viewBinder.bind(viewGroup)
                attachedObserver?.onAttached(mapboxNavigation)
            }
        }.invokeOnCompletion {
            attachedObserver?.onDetached(mapboxNavigation)
            viewGroup.removeAllViews()
        }
    }

    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
    }

    /**
     * Create your flowable [UIBinder]. This allows you to use a flowable state to
     * determine what is being shown in the [viewGroup].
     */
    abstract fun MapboxNavigation.flowViewBinders(): Flow<UIBinder>
}
