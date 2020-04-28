package com.mapbox.navigation.core.telemetry.events

import androidx.annotation.StringDef

/**
 * Scope of user's feedback events that might be sent through [com.mapbox.navigation.core.MapboxNavigation.postUserFeedback].
 *
 * This event occurs if the user taps a feedback button in the navigation app indicating there was a problem.
 */
class FeedbackEvent {
    companion object {
        /**
         * Feedback type *general*: when no one else is not fit or actually *general* feedback
         */
        const val FEEDBACK_TYPE_GENERAL_ISSUE = "general"

        /**
         * Feedback type *accident*: crash, uneven surface, etc
         */
        const val FEEDBACK_TYPE_ACCIDENT = "accident"

        /**
         * Feedback type *hazard*: such as debris, stopped vehicles, etc
         */
        const val FEEDBACK_TYPE_HAZARD = "hazard"

        /**
         * Feedback type *road closed*: closed road or one that does not allow vehicles
         */
        const val FEEDBACK_TYPE_ROAD_CLOSED = "road_closed"

        /**
         * Feedback type *not allowed*: a turn/maneuver that isn't allowed
         */
        const val FEEDBACK_TYPE_NOT_ALLOWED = "not_allowed"

        /**
         * Feedback type *routing error*: poor instruction or route choice
         * (ambiguous or poorly-timed turn announcement, or a set of confusing turns)
         */
        const val FEEDBACK_TYPE_ROUTING_ERROR = "routing_error"

        /**
         * Feedback type *missing road*: road doesn't exist
         */
        const val FEEDBACK_TYPE_MISSING_ROAD = "missing_road"

        /**
         * Feedback type *missing exit*: dead end
         */
        const val FEEDBACK_TYPE_MISSING_EXIT = "missing_exit"

        /**
         * Feedback type *confusing instruction*: wrong voice/text instruction
         */
        const val FEEDBACK_TYPE_CONFUSING_INSTRUCTION = "confusing_instruction"

        /**
         * Feedback type *inaccurate gps*: wrong location of navigation puck on the map
         */
        const val FEEDBACK_TYPE_INACCURATE_GPS = "inaccurate_gps"

        /**
         * Feedback type *incorrect visual guidance*: wrong visual guidance
         */
        const val FEEDBACK_TYPE_INCORRECT_VISUAL_GUIDANCE = "incorrect_visual_guidance"

        /**
         * Feedback type *incorrect audio guidance*: wrong audio guidance
         */
        const val FEEDBACK_TYPE_INCORRECT_AUDIO_GUIDANCE = "incorrect_audio_guidance"

        /**
         * Feedback source *reroute*: the user tapped a feedback button in response to a reroute
         */
        const val FEEDBACK_SOURCE_REROUTE = "reroute"

        /**
         * Feedback source *user*: the user tapped a feedback button
         */
        const val FEEDBACK_SOURCE_UI = "user"

        /**
         * Feedback description for *incorrect visual guidance*: turn icon incorrect
         */
        const val FEEDBACK_DESCRIPTION_TURN_ICON_INCORRECT = "turn_icon_incorrect"

        /**
         * Feedback description for *incorrect visual guidance*: street name incorrect
         */
        const val FEEDBACK_DESCRIPTION_STREET_NAME_INCORRECT = "street_name_incorrect"

        /**
         * Feedback description for *incorrect visual guidance*: instruction unnecessary
         */
        const val FEEDBACK_DESCRIPTION_INSTRUCTION_UNNECESSARY = "instruction_unnecessary"

        /**
         * Feedback description for *incorrect visual guidance*: instruction missing
         */
        const val FEEDBACK_DESCRIPTION_INSTRUCTION_MISSING = "instruction_missing"

        /**
         * Feedback description for *incorrect visual guidance*: maneuver incorrect
         */
        const val FEEDBACK_DESCRIPTION_MANEUVER_INCORRECT = "maneuver_incorrect"

        /**
         * Feedback description for *incorrect visual guidance*: exit info incorrect
         */
        const val FEEDBACK_DESCRIPTION_EXIT_INFO_INCORRECT = "exit_info_incorrect"

        /**
         * Feedback description for *incorrect visual guidance*: lane guidance incorrect
         */
        const val FEEDBACK_DESCRIPTION_LANE_GUIDANCE_INCORRECT = "lane_guidance_incorrect"

        /**
         * Feedback description for *incorrect visual guidance*: road know by different name
         */
        const val FEEDBACK_DESCRIPTION_ROAD_KNOW_BY_DIFFERENT_NAME = "road_know_by_different_name"

        /**
         * Feedback description for *incorrect audio guidance*: guidance too early
         */
        const val FEEDBACK_DESCRIPTION_GUIDANCE_TOO_EARLY = "guidance_too_early"

        /**
         * Feedback description for *incorrect audio guidance*: guidance too late
         */
        const val FEEDBACK_DESCRIPTION_GUIDANCE_TOO_LATE = "guidance_too_late"

        /**
         * Feedback description for *incorrect audio guidance*: pronunciation incorrect
         */
        const val FEEDBACK_DESCRIPTION_PRONUNCIATION_INCORRECT = "pronunciation_incorrect"

        /**
         * Feedback description for *incorrect audio guidance*: road name repeated
         */
        const val FEEDBACK_DESCRIPTION_ROAD_NAME_REPEATED = "road_name_repeated"

        /**
         * Feedback description for *routing error*: route not drive-able
         */
        const val FEEDBACK_DESCRIPTION_ROUTE_NOT_DRIVE_ABLE = "route_not_drive_able"

        /**
         * Feedback description for *routing error*: route not preferred
         */
        const val FEEDBACK_DESCRIPTION_ROUTE_NOT_PREFERRED = "route_not_preferred"

        /**
         * Feedback description for *routing error*: alternative route not expected
         */
        const val FEEDBACK_DESCRIPTION_ALTERNATIVE_ROUTE_NOT_EXPECTED =
            "alternative_route_not_expected"

        /**
         * Feedback description for *routing error*: route included missing roads
         */
        const val FEEDBACK_DESCRIPTION_ROUTE_INCLUDED_MISSING_ROADS = "route_included_missing_roads"

        /**
         * Feedback description for *routing error*: route had roads too narrow to pass
         */
        const val FEEDBACK_DESCRIPTION_ROUTE_HAD_ROADS_TOO_NARROW_TO_PASS =
            "route_had_roads_too_narrow_to_pass"

        /**
         * Feedback description for *routing error*: routed down a one-way
         */
        const val FEEDBACK_DESCRIPTION_ROUTED_DOWN_A_ONE_WAY = "routed_down_a_one_way"

        /**
         * Feedback description for *routing error*: turn was not allowed
         */
        const val FEEDBACK_DESCRIPTION_TURN_WAS_NOT_ALLOWED = "turn_was_not_allowed"

        /**
         * Feedback description for *routing error*: cars not allowed on street
         */
        const val FEEDBACK_DESCRIPTION_CARS_NOT_ALLOWED_ON_STREET = "cars_not_allowed_on_street"

        /**
         * Feedback description for *routing error*: turn at intersection was unprotected
         */
        const val FEEDBACK_DESCRIPTION_TURN_AT_INTERSECTION_WAS_UNPROTECTED =
            "turn_at_intersection_was_unprotected"

        /**
         * Feedback description for *routing error*: street permanently blocked off
         */
        const val FEEDBACK_DESCRIPTION_STREET_PERMANENTLY_BLOCKED_OFF =
            "street_permanently_blocked_off"

        /**
         * Feedback description for *routing error*: road is missing from map
         */
        const val FEEDBACK_DESCRIPTION_ROAD_IS_MISSING_FROM_MAP = "road_is_missing_from_map"
    }

    /**
     * Type of feedback mean WHAT happen
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        FEEDBACK_TYPE_GENERAL_ISSUE,
        FEEDBACK_TYPE_ACCIDENT,
        FEEDBACK_TYPE_HAZARD,
        FEEDBACK_TYPE_ROAD_CLOSED,
        FEEDBACK_TYPE_NOT_ALLOWED,
        FEEDBACK_TYPE_ROUTING_ERROR,
        FEEDBACK_TYPE_CONFUSING_INSTRUCTION,
        FEEDBACK_TYPE_INACCURATE_GPS,
        FEEDBACK_TYPE_MISSING_ROAD,
        FEEDBACK_TYPE_MISSING_EXIT
    )
    annotation class FeedbackType

    /**
     * Feedback source mean WHERE happen
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        FEEDBACK_SOURCE_REROUTE,
        FEEDBACK_SOURCE_UI
    )
    annotation class FeedbackSource

    /**
     * Detail description for different feedback type
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        FEEDBACK_DESCRIPTION_TURN_ICON_INCORRECT,
        FEEDBACK_DESCRIPTION_STREET_NAME_INCORRECT,
        FEEDBACK_DESCRIPTION_INSTRUCTION_UNNECESSARY,
        FEEDBACK_DESCRIPTION_INSTRUCTION_MISSING,
        FEEDBACK_DESCRIPTION_MANEUVER_INCORRECT,
        FEEDBACK_DESCRIPTION_EXIT_INFO_INCORRECT,
        FEEDBACK_DESCRIPTION_LANE_GUIDANCE_INCORRECT,
        FEEDBACK_DESCRIPTION_ROAD_KNOW_BY_DIFFERENT_NAME,
        FEEDBACK_DESCRIPTION_GUIDANCE_TOO_EARLY,
        FEEDBACK_DESCRIPTION_GUIDANCE_TOO_LATE,
        FEEDBACK_DESCRIPTION_PRONUNCIATION_INCORRECT,
        FEEDBACK_DESCRIPTION_ROAD_NAME_REPEATED,
        FEEDBACK_DESCRIPTION_ROUTE_NOT_DRIVE_ABLE,
        FEEDBACK_DESCRIPTION_ROUTE_NOT_PREFERRED,
        FEEDBACK_DESCRIPTION_ALTERNATIVE_ROUTE_NOT_EXPECTED,
        FEEDBACK_DESCRIPTION_ROUTE_INCLUDED_MISSING_ROADS,
        FEEDBACK_DESCRIPTION_ROUTE_HAD_ROADS_TOO_NARROW_TO_PASS,
        FEEDBACK_DESCRIPTION_ROUTED_DOWN_A_ONE_WAY,
        FEEDBACK_DESCRIPTION_TURN_WAS_NOT_ALLOWED,
        FEEDBACK_DESCRIPTION_CARS_NOT_ALLOWED_ON_STREET,
        FEEDBACK_DESCRIPTION_TURN_AT_INTERSECTION_WAS_UNPROTECTED,
        FEEDBACK_DESCRIPTION_STREET_PERMANENTLY_BLOCKED_OFF,
        FEEDBACK_DESCRIPTION_ROAD_IS_MISSING_FROM_MAP
    )
    annotation class FeedbackDescription
}
