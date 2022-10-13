package com.mapbox.navigation.ui.app.internal

import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.ui.app.internal.audioguidance.AudioGuidanceState
import com.mapbox.navigation.ui.app.internal.camera.CameraState
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterState

/**
 * Navigation state for internal use.
 */
data class State constructor(
    val destination: Destination? = null,
    val location: LocationMatcherResult? = null,
    val navigation: NavigationState = NavigationState.FreeDrive,
    val camera: CameraState = CameraState(),
    val audio: AudioGuidanceState = AudioGuidanceState(),
    val tripSession: TripSessionStarterState = TripSessionStarterState()
)
