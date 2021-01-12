package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.ui.base.MapboxState

/**
 * Immutable object representing the maneuver data to be rendered.
 */
sealed class ManeuverState : MapboxState {

    /**
     * State representing data about the current maneuver.
     * @property maneuver Maneuver
     */
    data class CurrentManeuver(var maneuver: Maneuver) : ManeuverState()

    /**
     * Immutable object representing primary maneuver data to be rendered.
     */
    sealed class ManeuverPrimary : ManeuverState() {

        /**
         * State representing [BannerInstructions.primary].
         * @property maneuver PrimaryManeuver
         */
        data class Instruction(var maneuver: PrimaryManeuver) : ManeuverPrimary()

        /**
         * State allowing the view to be shown.
         */
        object Show : ManeuverPrimary()

        /**
         * State allowing the view to be hidden.
         */
        object Hide : ManeuverPrimary()
    }

    /**
     * Immutable object representing secondary maneuver data to be rendered.
     */
    sealed class ManeuverSecondary : ManeuverState() {

        /**
         * State representing [BannerInstructions.secondary].
         * @property maneuver SecondaryManeuver?
         */
        data class Instruction(var maneuver: SecondaryManeuver?) : ManeuverSecondary()

        /**
         * State allowing the view to be shown.
         */
        object Show : ManeuverSecondary()

        /**
         * State allowing the view to be hidden.
         */
        object Hide : ManeuverSecondary()
    }

    /**
     * Immutable object representing sub maneuver data to be rendered.
     */
    sealed class ManeuverSub : ManeuverState() {

        /**
         * State representing [BannerInstructions.sub].
         * @property maneuver SubManeuver?
         */
        data class Instruction(var maneuver: SubManeuver?) : ManeuverSub()

        /**
         * State allowing the view to be shown.
         */
        object Show : ManeuverSub()

        /**
         * State allowing the view to be hidden.
         */
        object Hide : ManeuverSub()
    }

    /**
     * Immutable object representing upcoming maneuver data to be rendered.
     */
    sealed class UpcomingManeuvers : ManeuverState() {

        /**
         * State representing list of upcoming maneuvers.
         * @property upcomingManeuverList List<Maneuver>
         */
        data class Upcoming(val upcomingManeuverList: List<Maneuver>) : UpcomingManeuvers()

        /**
         * State representing the current maneuver to be removed from list of upcoming maneuvers.
         * @property maneuver Maneuver
         */
        data class RemoveUpcoming(val maneuver: Maneuver) : UpcomingManeuvers()

        /**
         * State allowing the view to be shown.
         */
        object Show : UpcomingManeuvers()

        /**
         * State allowing the view to be hidden.
         */
        object Hide : UpcomingManeuvers()
    }

    /**
     * Immutable object representing lane guidance maneuver data to be rendered.
     */
    sealed class LaneGuidanceManeuver : ManeuverState() {

        /**
         * State representing the lane data.
         * @property lane Lane
         */
        data class AddLanes(val lane: Lane) : LaneGuidanceManeuver()

        /**
         * State to remove the lanes from UI.
         */
        object RemoveLanes : LaneGuidanceManeuver()

        /**
         * State allowing the view to be shown.
         */
        object Show : LaneGuidanceManeuver()

        /**
         * State allowing the view to be hidden.
         */
        object Hide : LaneGuidanceManeuver()
    }

    /**
     * State representing the distance remaining to finish the current step.
     * @property distanceFormatter DistanceFormatter to format the distance with proper units.
     * @property distanceRemaining Double
     */
    data class DistanceRemainingToFinishStep(
        val distanceFormatter: DistanceFormatter,
        val distanceRemaining: Double
    ) : ManeuverState()

    /**
     * State representing [BannerInstructions.distanceAlongGeometry] for a step.
     * @property distanceFormatter DistanceFormatter to format the distance with proper units.
     * @property totalStepDistance Double
     */
    data class TotalStepDistance(
        val distanceFormatter: DistanceFormatter,
        val totalStepDistance: Double
    ) : ManeuverState()
}
