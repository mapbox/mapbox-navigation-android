package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.navigation.dropin.model.Destination

internal data class RoutesState(
    val destination: Destination?,
    val navigationStarted: Boolean,
) {
    companion object {
        val INITIAL_STATE = RoutesState(null, false)
    }
}
