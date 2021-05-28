package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.maneuver.ManeuverAction
import com.mapbox.navigation.ui.maneuver.ManeuverProcessor
import com.mapbox.navigation.ui.maneuver.ManeuverResult.GetAllBannerInstructions
import com.mapbox.navigation.ui.maneuver.ManeuverResult.GetAllBannerInstructionsAfterStep
import com.mapbox.navigation.ui.maneuver.ManeuverResult.GetAllManeuvers
import com.mapbox.navigation.ui.maneuver.ManeuverResult.GetManeuver
import com.mapbox.navigation.ui.maneuver.ManeuverResult.GetStepDistanceRemaining
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Used to generate representation of maneuvers based on [BannerInstructions]
 * @property distanceFormatter DistanceFormatter
 */
class MapboxManeuverApi internal constructor(
    private val distanceFormatter: DistanceFormatter,
    private val processor: ManeuverProcessor
) {

    /**
     * @param formatter contains various instances for use in formatting distance related data
     * for display in the UI
     *
     * @return a [MapboxManeuverApi]
     */
    constructor(formatter: DistanceFormatter) : this(formatter, ManeuverProcessor())

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private var maneuverJob: Job? = null
    private var upcomingManeuverListJob: Job? = null
    private var stepDistanceRemainingJob: Job? = null

    /**
     * Given [BannerInstructions] the method goes through the list of [BannerComponents] and returns
     * a [Expected] wrapped inside [ManeuverCallback]
     * @param bannerInstruction BannerInstructions object representing [BannerInstructions]
     * @param callback ManeuverCallback contains [Expected]
     */
    fun getManeuver(
        bannerInstruction: BannerInstructions,
        callback: ManeuverCallback
    ) {
        maneuverJob?.cancel()
        maneuverJob = mainJobController.scope.launch {
            val action = ManeuverAction.GetManeuver(bannerInstruction)
            val result = processor.process(action) as GetManeuver
            callback.onManeuver(ExpectedFactory.createValue(result.maneuver))
        }
    }

    /**
     * Given [RouteStepProgress] the method returns the distance remaining to finish the [LegStep]
     * @param routeStepProgress RouteStepProgress progress object specific to the current step the user is on
     * @param callback StepDistanceRemainingCallback contains [Expected]
     */
    fun getStepDistanceRemaining(
        routeStepProgress: RouteStepProgress,
        callback: StepDistanceRemainingCallback
    ) {
        stepDistanceRemainingJob?.cancel()
        stepDistanceRemainingJob = mainJobController.scope.launch {
            val action = ManeuverAction.GetStepDistanceRemaining(routeStepProgress)
            val result = processor.process(action) as GetStepDistanceRemaining
            callback.onStepDistanceRemaining(
                ExpectedFactory.createValue(
                    StepDistance(distanceFormatter, result.distanceRemaining)
                )
            )
        }
    }

    /**
     * For a given [RouteLeg] the method returns all the maneuvers in the [LegStep] wrapped inside
     * [UpcomingManeuverListCallback].
     * @param routeProgress RouteProgress
     * @param callback UpcomingManeuverListCallback contains [Expected]
     */
    fun getUpcomingManeuverList(
        routeProgress: RouteProgress,
        callback: UpcomingManeuverListCallback
    ) {
        upcomingManeuverListJob?.cancel()
        upcomingManeuverListJob = mainJobController.scope.launch {
            val allBannersAction = ManeuverAction.GetAllBannerInstructions(routeProgress)
            val allBanners = processor.process(allBannersAction) as GetAllBannerInstructions
            val allBannersAfterStepAction =
                ManeuverAction.GetAllBannerInstructionsAfterStep(
                    routeProgress,
                    allBanners.bannerInstructions
                )
            val allBannersAfterStep = processor
                .process(allBannersAfterStepAction) as GetAllBannerInstructionsAfterStep
            val allManeuversAction =
                ManeuverAction.GetAllManeuvers(allBannersAfterStep.bannerInstructions)
            val allManeuvers = processor.process(allManeuversAction) as GetAllManeuvers
            callback.onUpcomingManeuvers(ExpectedFactory.createValue(allManeuvers.maneuverList))
        }
    }

    /**
     * The function cancels the current job [getManeuver] and doesn't return the callback
     * associated with it.
     */
    fun cancelManeuver() {
        maneuverJob?.cancel()
    }

    /**
     * The function cancels the current job [getUpcomingManeuverList] and doesn't return the
     * callback associated with it.
     */
    fun cancelUpcomingManeuverList() {
        upcomingManeuverListJob?.cancel()
    }

    /**
     * The function cancels the current job [getStepDistanceRemaining] and doesn't return
     * the callback associated with it.
     */
    fun cancelStepDistanceRemaining() {
        stepDistanceRemainingJob?.cancel()
    }
}
