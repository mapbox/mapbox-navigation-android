package com.mapbox.navigation.dropin.component.infopanel

import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelHeaderComponent(
    private val binding: MapboxInfoPanelHeaderLayoutBinding,
    private val navigationStateViewModel: NavigationStateViewModel,
    private val destinationViewModel: DestinationViewModel,
    private val locationViewModel: LocationViewModel,
    private val routesViewModel: RoutesViewModel,
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
        navigationStateViewModel.state.observe {
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

        destinationViewModel.state.observe {
            val placeName = it.destination?.features?.firstOrNull()?.placeName()
            binding.poiName.text = placeName
                ?: resources.getString(R.string.mapbox_drop_in_dropped_pin)
        }

        binding.routePreview.setOnClickListener {
            coroutineScope.launch {
                if (fetchRouteIfNeeded()) {
                    navigationStateViewModel.invoke(
                        NavigationStateAction.Update(NavigationState.RoutePreview)
                    )
                }
            }
        }

        binding.startNavigation.setOnClickListener {
            coroutineScope.launch {
                if (fetchRouteIfNeeded()) {
                    navigationStateViewModel.invoke(
                        NavigationStateAction.Update(NavigationState.ActiveNavigation)
                    )
                }
            }
        }

        binding.endNavigation.setOnClickListener {
            routesViewModel.invoke(RoutesAction.SetRoutes(emptyList()))
            destinationViewModel.invoke(DestinationAction.SetDestination(null))
            navigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.FreeDrive)
            )
        }
    }

    /**
     * Dispatch FetchPoints action and wait for RoutesState.Ready.
     * Method returns immediately if already in RoutesState.Ready or RoutesState.Fetching, or if
     * required location or destination data is missing.
     *
     * @return `true` once in RoutesState.Ready state, otherwise `false`
     */
    private suspend fun fetchRouteIfNeeded(): Boolean {
        if (routesViewModel.state.value is RoutesState.Ready) return true
        if (routesViewModel.state.value is RoutesState.Fetching) return false

        return ifNonNull(
            locationViewModel.lastPoint,
            destinationViewModel.state.value.destination
        ) { lastPoint, destination ->
            routesViewModel.invoke(RoutesAction.FetchPoints(listOf(lastPoint, destination.point)))
            waitForReady()
        } ?: false
    }

    private suspend fun waitForReady(): Boolean {
        routesViewModel.state.takeWhile { it is RoutesState.Fetching }.collect()
        return routesViewModel.state.value is RoutesState.Ready
    }
}
