package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError

/**
 * Interface definition for a callback to be invoked when a upcoming maneuver data is processed.
 */
fun interface UpcomingManeuverListCallback {

    /**
     * Invoked when all the upcoming maneuvers are ready.
     * @param maneuvers UpcomingManeuvers represents the upcoming maneuvers to be rendered on the view.
     */
    fun onUpcomingManeuvers(maneuvers: Expected<List<Maneuver>, ManeuverError>)
}
