package com.mapbox.navigation.dropin.internal.extensions

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.Flow

/**
 * High Order Component that observes [flow] changes and re-creates child component
 * using [factory] function.
 *
 * usage eg.
 * ```
 *   ReloadingComponent(myFlowValue) { value ->
 *      MyComponent(value)
 *   }
 * ```
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class ReloadingComponent<T>(
    private val flow: Flow<T>,
    private val factory: (T) -> UIComponent?
) : UIComponent() {
    private var childComponent: UIComponent? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        flow.observe {
            childComponent?.onDetached(mapboxNavigation)
            childComponent = factory(it)
            childComponent?.onAttached(mapboxNavigation)
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        childComponent?.onDetached(mapboxNavigation)
        childComponent = null
    }
}
