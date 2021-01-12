package com.mapbox.navigation.ui.base.api.maneuver

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState

/**
 * An Api that allows you to generate upcoming maneuvers for a given [RouteLeg].
 */
interface UpcomingManeuverApi {

    /**
     * For a given [RouteLeg] the method returns all the maneuvers in the [LegStep] wrapped inside
     * [UpcomingManeuversCallback].
     * @param routeLeg RouteLeg route between two points.
     * @param callback UpcomingManeuversCallback contains [ManeuverState.UpcomingManeuvers]
     */
    fun retrieveUpcomingManeuvers(
        routeLeg: RouteLeg,
        callback: UpcomingManeuversCallback
    )

    /**
     * The function cancels the current job [retrieveUpcomingManeuvers] and doesn't return the
     * callback associated with it.
     */
    fun cancelUpcomingManeuver()
}
