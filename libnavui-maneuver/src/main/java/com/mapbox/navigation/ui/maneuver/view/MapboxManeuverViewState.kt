package com.mapbox.navigation.ui.maneuver.view

/**
 * The class informs the user about the state of [MapboxManeuverView].
 */
sealed class MapboxManeuverViewState {
    /**
     * Notifies the user that [MapboxManeuverView] is in `Expanded` state
     */
    object EXPANDED : MapboxManeuverViewState()

    /**
     * Notifies the user that [MapboxManeuverView] is in `Collapsed` state
     */
    object COLLAPSED : MapboxManeuverViewState()
}
