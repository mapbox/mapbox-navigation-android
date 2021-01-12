package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.base.api.maneuver.ManeuverApi
import com.mapbox.navigation.ui.base.api.maneuver.ManeuverCallback
import com.mapbox.navigation.ui.base.api.maneuver.StepDistanceRemainingCallback
import com.mapbox.navigation.ui.base.api.maneuver.UpcomingManeuversCallback
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState.CurrentManeuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState.DistanceRemainingToFinishStep
import com.mapbox.navigation.ui.maneuver.ManeuverAction
import com.mapbox.navigation.ui.maneuver.ManeuverProcessor
import com.mapbox.navigation.ui.maneuver.ManeuverResult
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Implementation of [ManeuverApi] allowing access to generate representation of
 * maneuvers based on [BannerInstructions]
 * @property distanceFormatter DistanceFormatter
 */
class MapboxManeuverApi internal constructor(
    private val distanceFormatter: DistanceFormatter,
    private val processor: ManeuverProcessor
) : ManeuverApi {

    /**
     * @param formatter contains various instances for use in formatting distance related data
     * for display in the UI
     *
     * @return a [MapboxManeuverApi]
     */
    constructor(formatter: DistanceFormatter) : this(formatter, ManeuverProcessor)

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private var currentManeuverJob: Job? = null
    private var upcomingManeuverJob: Job? = null
    private var stepDistanceRemainingJob: Job? = null

    /**
     * Given [BannerInstructions] the method goes through the list of [BannerComponents] and returns
     * a [Maneuver] wrapped inside [ManeuverCallback]
     * @param bannerInstruction BannerInstructions object representing [BannerInstructions]
     * @param callback ManeuverCallback contains [ManeuverState.CurrentManeuver]
     */
    override fun retrieveManeuver(
        bannerInstruction: BannerInstructions,
        callback: ManeuverCallback
    ) {
        currentManeuverJob?.cancel()
        currentManeuverJob = mainJobController.scope.launch {
            val action = ManeuverAction.ParseCurrentManeuver(bannerInstruction)
            val result = processor.process(action) as ManeuverResult.CurrentManeuver
            callback.onManeuver(CurrentManeuver(result.currentManeuver))
        }
    }

    /**
     * Given [RouteStepProgress] the method returns the distance remaining to finish the [LegStep]
     * @param routeStepProgress RouteStepProgress progress object specific to the current step the user is on
     * @param callback StepDistanceRemainingCallback contains [ManeuverState.DistanceRemainingToFinishStep]
     */
    override fun retrieveStepDistanceRemaining(
        routeStepProgress: RouteStepProgress,
        callback: StepDistanceRemainingCallback
    ) {
        stepDistanceRemainingJob?.cancel()
        stepDistanceRemainingJob = mainJobController.scope.launch {
            val action = ManeuverAction.FindStepDistanceRemaining(routeStepProgress)
            val result = processor.process(action) as ManeuverResult.StepDistanceRemaining
            callback.onStepDistanceRemaining(
                DistanceRemainingToFinishStep(
                    distanceFormatter,
                    result.distanceRemaining
                )
            )
        }
    }

    /**
     * For a given [RouteLeg] the method returns all the maneuvers in the [LegStep] wrapped inside
     * [UpcomingManeuversCallback].
     * @param routeLeg RouteLeg route between two points.
     * @param callback UpcomingManeuversCallback contains [ManeuverState.UpcomingManeuvers]
     */
    override fun retrieveUpcomingManeuvers(
        routeLeg: RouteLeg,
        callback: UpcomingManeuversCallback
    ) {
        upcomingManeuverJob?.cancel()
        upcomingManeuverJob = mainJobController.scope.launch {
            val action = ManeuverAction.FindAllUpcomingManeuvers(routeLeg)
            val result = processor.process(action) as ManeuverResult.UpcomingManeuvers
            callback.onUpcomingManeuvers(
                ManeuverState.UpcomingManeuvers.Upcoming(result.upcomingManeuverList)
            )
        }
    }

    /**
     * The function cancels the current job [retrieveManeuver] and doesn't return the callback
     * associated with it.
     */
    override fun cancelManeuver() {
        currentManeuverJob?.cancel()
    }

    /**
     * The function cancels the current job [retrieveUpcomingManeuvers] and doesn't return the
     * callback associated with it.
     */
    override fun cancelUpcomingManeuver() {
        upcomingManeuverJob?.cancel()
    }

    /**
     * The function cancels the current job [retrieveStepDistanceRemaining] and doesn't return
     * the callback associated with it.
     */
    override fun cancelStepDistanceRemaining() {
        stepDistanceRemainingJob?.cancel()
    }
}
