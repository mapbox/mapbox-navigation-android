package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ManeuverBehavior {

    private val _maneuverBehavior = MutableStateFlow<MapboxManeuverViewState>(
        MapboxManeuverViewState.COLLAPSED
    )
    val maneuverBehavior = _maneuverBehavior.asStateFlow()

    fun updateBehavior(newState: MapboxManeuverViewState) {
        _maneuverBehavior.value = newState
    }
}
