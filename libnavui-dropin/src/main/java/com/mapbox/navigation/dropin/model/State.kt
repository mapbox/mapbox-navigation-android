package com.mapbox.navigation.dropin.model

import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.component.audioguidance.AudioGuidanceState
import com.mapbox.navigation.dropin.component.camera.CameraState
import com.mapbox.navigation.dropin.component.destination.Destination
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.component.tripsession.TripSessionStarterState

/**
 * Drop-in UI state.
 */
internal data class State(
    val destination: Destination? = null,
    val location: LocationMatcherResult? = null,
    val navigation: NavigationState = NavigationState.FreeDrive,
    val camera: CameraState = CameraState(),
    val audio: AudioGuidanceState = AudioGuidanceState(),
    val routes: RoutesState = RoutesState.Empty,
    val tripSession: TripSessionStarterState = TripSessionStarterState()
)
