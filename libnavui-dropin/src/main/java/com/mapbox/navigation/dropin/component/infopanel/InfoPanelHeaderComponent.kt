package com.mapbox.navigation.dropin.component.infopanel

import androidx.core.view.isVisible
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
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class InfoPanelHeaderComponent(
    private val binding: MapboxInfoPanelHeaderLayoutBinding,
    private val navigationStateViewModel: NavigationStateViewModel,
    private val destinationViewModel: DestinationViewModel,
    private val locationViewModel: LocationViewModel,
    private val routesViewModel: RoutesViewModel,
) : UIComponent() {

    private val resources get() = binding.root.resources

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

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
            updateNavigationStateWhenRouteIsReady(NavigationState.RoutePreview)
        }

        binding.startNavigation.setOnClickListener {
            updateNavigationStateWhenRouteIsReady(NavigationState.ActiveNavigation)
        }

        binding.endNavigation.setOnClickListener {
            routesViewModel.invoke(RoutesAction.SetRoutes(emptyList()))
            destinationViewModel.invoke(DestinationAction.SetDestination(null))
            navigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.FreeDrive)
            )
        }
    }

    private fun updateNavigationStateWhenRouteIsReady(navigationState: NavigationState) {
        ifNonNull(
            locationViewModel.lastPoint,
            destinationViewModel.state.value.destination
        ) { lastPoint, destination ->
            routesViewModel.invoke(RoutesAction.FetchPoints(listOf(lastPoint, destination.point)))
            coroutineScope.launch {
                // Wait for fetching to complete and then take action
                val isRouteReady = waitForFetched()
                if (isActive && isRouteReady) {
                    navigationStateViewModel.invoke(
                        NavigationStateAction.Update(navigationState)
                    )
                } else {
                    logW(TAG, "Routes are not ready")
                }
            }
        } ?: logE(TAG, "Cannot fetch routes because state is incorrect")
    }

    private suspend fun waitForFetched(): Boolean {
        routesViewModel.state.takeWhile { it is RoutesState.Fetching }.collect()
        return routesViewModel.state.value is RoutesState.Ready
    }

    private companion object {
        private val TAG = this::class.java.simpleName
    }
}
