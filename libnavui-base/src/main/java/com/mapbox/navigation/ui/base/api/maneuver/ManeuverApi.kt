package com.mapbox.navigation.ui.base.api.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState

/**
 * An Api that allows you to generate maneuvers based on [BannerInstructions]
 */
interface ManeuverApi : UpcomingManeuverApi {

    /**
     * Given [BannerInstructions] the method goes through the list of [BannerComponents] and returns
     * a [Maneuver] wrapped inside [ManeuverCallback]
     * @param bannerInstruction BannerInstructions object representing [BannerInstructions]
     * @param callback ManeuverCallback contains [ManeuverState.CurrentManeuver]
     */
    fun retrieveManeuver(
        bannerInstruction: BannerInstructions,
        callback: ManeuverCallback
    )

    /**
     * Given [RouteStepProgress] the method returns the distance remaining to finish the [LegStep]
     * @param routeStepProgress RouteStepProgress progress object specific to the current step the user is on
     * @param callback StepDistanceRemainingCallback contains [ManeuverState.DistanceRemainingToFinishStep]
     */
    fun retrieveStepDistanceRemaining(
        routeStepProgress: RouteStepProgress,
        callback: StepDistanceRemainingCallback
    )

    /**
     * The function cancels the current job [retrieveManeuver] and doesn't return the callback
     * associated with it.
     */
    fun cancelManeuver()

    /**
     * The function cancels the current job [retrieveStepDistanceRemaining] and doesn't return
     * the callback associated with it.
     */
    fun cancelStepDistanceRemaining()
}
