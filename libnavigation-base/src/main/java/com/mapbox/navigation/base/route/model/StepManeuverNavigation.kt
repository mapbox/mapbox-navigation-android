package com.mapbox.navigation.base.route.model

import androidx.annotation.StringDef

data class StepManeuverNavigation(
    val location: PointNavigation,
    val bearingBefore: Double?,
    val bearingAfter: Double?,
    @StepManeuverTypeNavigation val type: String?,
    val exit: Int?
) {
    companion object {

        /**
         * A basic turn in the direction of the modifier.
         *
         * @since 1.0
         */
        const val TURN = "turn"

        /**
         * The road name changes (after a mandatory turn).
         *
         * @since 1.0
         */
        const val NEW_NAME = "new name"

        /**
         * Indicates departure from a leg.
         * The  modifier value indicates the position of the departure point
         * in relation to the current direction of travel.
         *
         * @since 1.0
         */
        const val DEPART = "depart"

        /**
         * Indicates arrival to a destination of a leg.
         * The modifier value indicates the position of the arrival point
         * in relation to the current direction of travel.
         *
         * @since 1.0
         */
        const val ARRIVE = "arrive"

        /**
         * Merge onto a street.
         *
         * @since 1.0
         */
        const val MERGE = "merge"

        /**
         * Take a ramp to enter a highway.
         * @since 1.0
         */
        const val ON_RAMP = "on ramp"

        /**
         * Take a ramp to exit a highway.
         *
         * @since 1.0
         */
        const val OFF_RAMP = "off ramp"

        /**
         * Take the left or right side of a fork.
         *
         * @since 1.0
         */
        const val FORK = "fork"

        /**
         * Road ends in a T intersection.
         *
         * @since 1.0
         */
        const val END_OF_ROAD = "end of road"

        /**
         * Continue on a street after a turn.
         *
         * @since 1.0
         */
        const val CONTINUE = "continue"

        /**
         * Traverse roundabout.
         * Has an additional property  exit in the route step that contains
         * the exit number. The  modifier specifies the direction of entering the roundabout.
         *
         * @since 1.0
         */
        const val ROUNDABOUT = "roundabout"

        /**
         * A traffic circle. While very similar to a larger version of a roundabout,
         * it does not necessarily follow roundabout rules for right of way.
         *
         * @since 1.0
         */
        const val ROTARY = "rotary"

        /**
         * A small roundabout that is treated as an intersection.
         *
         * @since 1.0
         */
        const val ROUNDABOUT_TURN = "roundabout turn"

        /**
         * Indicates a change of driving conditions, for example changing the  mode
         * from driving to ferry.
         *
         * @since 1.0
         */
        const val NOTIFICATION = "notification"

        /**
         * Indicates the exit maneuver from a roundabout.
         *
         * @since 1.0
         */
        const val EXIT_ROUNDABOUT = "exit roundabout"

        /**
         * Indicates the exit maneuver from a rotary.
         *
         * @since 1.0
         */
        const val EXIT_ROTARY = "exit rotary"
    }

    @Retention(AnnotationRetention.RUNTIME)
    @StringDef(
        TURN,
        NEW_NAME,
        DEPART,
        ARRIVE,
        MERGE,
        ON_RAMP,
        OFF_RAMP,
        FORK,
        END_OF_ROAD,
        CONTINUE,
        ROUNDABOUT,
        ROTARY,
        ROUNDABOUT_TURN,
        NOTIFICATION,
        EXIT_ROUNDABOUT,
        EXIT_ROTARY
    )
    annotation class StepManeuverTypeNavigation
}
