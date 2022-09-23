package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ManeuverBehavior {

    private val _maneuverBehavior = MutableStateFlow<MapboxManeuverViewState>(
        MapboxManeuverViewState.COLLAPSED
    )
    private val _maneuverViewHeight = MutableStateFlow(0)

    val maneuverBehavior = _maneuverBehavior.asStateFlow()
    val maneuverViewHeight = _maneuverViewHeight.asStateFlow()

    fun updateBehavior(newState: MapboxManeuverViewState) {
        _maneuverBehavior.value = newState
    }

    fun updateViewHeight(newHeight: Int) {
        _maneuverViewHeight.value = newHeight
    }
}
