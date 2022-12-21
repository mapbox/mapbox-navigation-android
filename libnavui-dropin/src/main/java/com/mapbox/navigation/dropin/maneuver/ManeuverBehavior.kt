package com.mapbox.navigation.dropin.maneuver

import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ManeuverBehavior {

    private val _maneuverViewState = MutableStateFlow<MapboxManeuverViewState>(
        MapboxManeuverViewState.COLLAPSED
    )
    private val _maneuverViewVisibility = MutableStateFlow(false)

    val maneuverViewState = _maneuverViewState.asStateFlow()
    val maneuverViewVisibility = _maneuverViewVisibility.asStateFlow()

    fun updateBehavior(newState: MapboxManeuverViewState) {
        _maneuverViewState.value = newState
    }

    fun updateViewVisibility(visibility: Boolean) {
        _maneuverViewVisibility.value = visibility
    }
}
