package com.mapbox.navigation.base.route.model

import androidx.annotation.StringDef
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.StepManeuverNavigation.StepManeuverTypeNavigation

/**
 *
 * @property location A [Point] representing this intersection location.
 * @since 1.0
 *
 * @property bearingBefore Number between 0 and 360 indicating the clockwise angle from true north
 * to the direction of travel right before the maneuver.
 * @since 1.0
 *
 * @property bearingAfter Number between 0 and 360 indicating the clockwise angle from true north
 * to the direction of travel right after the maneuver.
 * @since 1.0
 *
 * @property instruction A human-readable instruction of how to execute the returned maneuver. This String is built
 * using OSRM-Text-Instructions and can be further customized inside either the Mapbox Navigation
 * SDK for Android or using the OSRM-Text-Instructions.java project in Project-OSRM.
 * @see [Navigation SDK](https://github.com/mapbox/mapbox-navigation-android)
 * @see [OSRM-Text-Instructions.java](https://github.com/Project-OSRM/osrm-text-instructions.java)
 * @since 1.0
 *
 * @property type This indicates the type of maneuver.
 * @see StepManeuverTypeNavigation
 * @since 1.0
 *
 * @property modifier This indicates the mode of the maneuver. If type is of turn, the modifier indicates the
 * change in direction accomplished through the turn. If the type is of depart/arrive, the
 * modifier indicates the position of waypoint from the current direction of travel.
 * @since 1.0
 *
 * @property exit An optional integer indicating number of the exit to take. If exit is undefined the destination
 * is on the roundabout. The property exists for the following type properties:
 *
 * else - indicates the number of intersections passed until the turn.
 * roundabout - traverse roundabout
 * rotary - a traffic circle
 * @since 1.0
 */
class StepManeuverNavigation(
    val location: Point,
    val bearingBefore: Double?,
    val bearingAfter: Double?,
    val instruction: String?,
    @StepManeuverTypeNavigation
    val type: String?,
    val modifier: String?,
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
         * It can offer [LegStep.rotaryName]  parameters,
         * [LegStep.rotaryPronunciation] ()}  parameters, or both,
         * in addition to the [.exit] property.
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
         * Will not appear in results unless you supply true to the [.exit] query
         * parameter in the request.
         *
         * @since 1.0
         */
        const val EXIT_ROUNDABOUT = "exit roundabout"

        /**
         * Indicates the exit maneuver from a rotary.
         * Will not appear in results unless you supply true
         * to the [MapboxDirections.roundaboutExits]  query parameter in the request.
         *
         * @since 1.0
         */
        const val EXIT_ROTARY = "exit rotary"
    }

    @Retention(AnnotationRetention.SOURCE)
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
