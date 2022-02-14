package com.mapbox.navigation.dropin.component.replay

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.extensions.flowTripSessionState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * This component is used to hook up replay. A different component could be used to hook up a real
 * navigation trip session.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DropInReplayButton(
    dropInNavigationViewContext: DropInNavigationViewContext,
    private val startNavigation: View,
) : UIComponent() {
    private val replayComponent = dropInNavigationViewContext.viewModel.replayComponent
    private val viewModel = dropInNavigationViewContext.viewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun View.clicks() = callbackFlow {
        setOnClickListener {
            trySend(Unit)
        }
        awaitClose { setOnClickListener(null) }
    }

    // Convert the MapboxNavigation state into button visibility
    private fun MapboxNavigation.flowVisibility(): Flow<Int> =
        combine(
            flowTripSessionState(), flowRoutesUpdated()
        ) { tripSessionState, routesUpdatedResult ->
            tripSessionState == TripSessionState.STOPPED && routesUpdatedResult.routes.isNotEmpty()
        }.onStart {
            // Make the button invisible when there are no events
            emit(false)
        }.map { if (it) View.VISIBLE else View.GONE }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        // Attach visibility for start replay button
        coroutineScope.launch {
            mapboxNavigation.flowVisibility().collect { startNavigation.visibility = it }
        }

        coroutineScope.launch {
            startNavigation.clicks().collect {
                replayComponent.startSimulation()
                mapboxNavigation.startReplayTripSession()

                // This will trigger the map to change ViewBinders. See it happen in
                // DropInNavigationViewCoordinator
                viewModel.updateState(NavigationState.ActiveNavigation)
            }
        }
    }
}
