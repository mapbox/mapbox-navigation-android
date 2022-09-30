package com.mapbox.navigation.dropin.tripsession

import android.annotation.SuppressLint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.ui.app.internal.SharedApp.tripSessionTransaction
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterState
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Component class that is responsible for starting and stopping [MapboxNavigation] trip session
 * and replay trip session when [TripSessionStarterState] changes.
 *
 * @param lifecycle Parent view Lifecycle to which this component is attached to.
 * @param store Store instance that holds NavigationState and TripSessionStarterState state
 */
@ExperimentalPreviewMapboxNavigationAPI
@SuppressLint("MissingPermission")
internal class TripSessionComponent(
    private val lifecycle: Lifecycle,
    private val store: Store
) : UIComponent() {

    private var replayRouteTripSession: ReplayRouteTripSession? = null

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     * @param mapboxNavigation
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flowStartReplaySession().collect { starterState ->
                    tripSessionTransaction {
                        if (starterState.isLocationPermissionGranted) {
                            if (starterState.isReplayEnabled) {
                                startReplayTripSession(mapboxNavigation)
                            } else {
                                startTripSession(mapboxNavigation)
                            }
                        } else {
                            mapboxNavigation.ensureTripSessionStopped()
                        }
                    }
                }
            }
        }
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     * @param mapboxNavigation
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        tripSessionTransaction {
            replayRouteTripSession?.stop(mapboxNavigation)
            replayRouteTripSession = null
        }
    }

    private fun flowStartReplaySession(): Flow<TripSessionStarterState> = combine(
        store.select { it.navigation },
        store.select { it.tripSession }
    ) { navigationState, tripSessionStarterState ->
        if (navigationState !is NavigationState.ActiveNavigation) {
            tripSessionStarterState.copy(isReplayEnabled = false)
        } else {
            tripSessionStarterState
        }
    }.distinctUntilChanged()

    private fun startTripSession(mapboxNavigation: MapboxNavigation) {
        replayRouteTripSession?.stop(mapboxNavigation)
        replayRouteTripSession = null
        mapboxNavigation.ensureTripSessionStarted()
    }

    private fun startReplayTripSession(mapboxNavigation: MapboxNavigation) {
        replayRouteTripSession?.stop(mapboxNavigation)
        replayRouteTripSession = ReplayRouteTripSession()
        replayRouteTripSession?.start(mapboxNavigation)
    }

    private fun MapboxNavigation.ensureTripSessionStarted() {
        if (getTripSessionState() != TripSessionState.STARTED) {
            startTripSession()
        }
    }

    private fun MapboxNavigation.ensureTripSessionStopped() {
        if (getTripSessionState() != TripSessionState.STOPPED) {
            stopTripSession()
        }
    }
}
