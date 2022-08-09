package com.mapbox.navigation.dropin.component.infopanel

import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.onClick
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.endNavigation
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.showRoutePreview
import com.mapbox.navigation.ui.app.internal.startActiveNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelHeaderComponent(
    private val store: Store,
    private val binding: MapboxInfoPanelHeaderLayoutBinding,
    @StyleRes private val routePreviewStyle: Int,
    @StyleRes private val endNavigationStyle: Int,
    @StyleRes private val startNavigationStyle: Int,
) : UIComponent() {

    private val resources get() = binding.root.resources

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        binding.routePreview.updateStyle(routePreviewStyle)
        binding.endNavigation.updateStyle(endNavigationStyle)
        binding.startNavigation.updateStyle(startNavigationStyle)

        // views visibility
        store.select { it.navigation }.observe {
            binding.poiName.isVisible = it == NavigationState.DestinationPreview
            binding.routePreview.isVisible = it == NavigationState.DestinationPreview
            binding.startNavigation.isVisible = it == NavigationState.DestinationPreview ||
                it == NavigationState.RoutePreview
            binding.endNavigation.isVisible = it == NavigationState.ActiveNavigation ||
                it == NavigationState.Arrival
            binding.tripProgressLayout.isVisible = it == NavigationState.ActiveNavigation ||
                it == NavigationState.RoutePreview
            binding.arrivedText.isVisible = it == NavigationState.Arrival
        }

        store.select { it.destination }.observe {
            val placeName = it?.features?.firstOrNull()?.placeName()
            binding.poiName.text = placeName
                ?: resources.getString(R.string.mapbox_drop_in_dropped_pin)
        }

        binding.routePreview.onClick(coroutineScope) {
            store.dispatch(showRoutePreview())
        }

        binding.startNavigation.onClick(coroutineScope) {
            store.dispatch(startActiveNavigation())
        }

        binding.endNavigation.onClick(coroutineScope) {
            store.dispatch(endNavigation())
        }
    }
}
