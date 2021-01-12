package com.mapbox.navigation.ui.base.api.maneuver

import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState

/**
 * Interface definition for a callback to be invoked when all the upcoming maneuvers is processed.
 */
interface UpcomingManeuversCallback {

    /**
     * Invoked when all the upcoming maneuvers are ready.
     * @param state UpcomingManeuvers represents the upcoming maneuvers to be rendered on the view.
     */
    fun onUpcomingManeuvers(state: ManeuverState.UpcomingManeuvers.Upcoming)
}
