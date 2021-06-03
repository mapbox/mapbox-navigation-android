package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError

/**
 * An interface that is triggered when maneuvers are ready.
 */
fun interface ManeuverCallback {

    /**
     * The method is invoked if there is a success or failure computing a list of [Maneuver] instructions.
     * @param maneuvers Expected with a value List<Maneuver> if success and an error value if failure.
     */
    fun onManeuvers(maneuvers: Expected<ManeuverError, List<Maneuver>>)
}
