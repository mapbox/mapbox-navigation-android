package com.mapbox.navigation.qa_test_app.lifecycle

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.dropin.extensions.flowTripSessionState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInReplayComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInStartReplayButton(
    private val view: View,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        view.visibility = View.VISIBLE

        coroutineScope.launch {
            view.clicks().collect {
                MapboxNavigationApp.getObserver(DropInReplayComponent::class).startSimulation()
                mapboxNavigation.startReplayTripSession()
            }
        }
        coroutineScope.launch {
            mapboxNavigation.flowTripSessionState().collect { tripSessionState ->
                view.visibility = when (tripSessionState) {
                    TripSessionState.STARTED -> View.GONE
                    TripSessionState.STOPPED -> View.VISIBLE
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        view.visibility = View.GONE
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun View.clicks(): Flow<View> = callbackFlow {
        setOnClickListener { trySend(it) }
        awaitClose { setOnClickListener(null) }
    }
}
