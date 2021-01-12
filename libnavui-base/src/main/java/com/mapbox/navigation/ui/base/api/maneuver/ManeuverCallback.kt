package com.mapbox.navigation.ui.base.api.maneuver

import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState

/**
 * Interface definition for a callback to be invoked when a [Maneuver] is processed.
 */
interface ManeuverCallback {

    /**
     * Invoked when [Maneuver] is ready.
     * @param currentManeuver CurrentManeuver represents maneuver to be represented on the view.
     */
    fun onManeuver(currentManeuver: ManeuverState.CurrentManeuver)
}
