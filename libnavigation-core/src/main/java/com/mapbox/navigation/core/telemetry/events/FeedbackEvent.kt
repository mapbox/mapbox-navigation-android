package com.mapbox.navigation.core.telemetry.events

import androidx.annotation.StringDef

/**
 * Scope of user's feedback events that might be sent through [com.mapbox.navigation.core.MapboxNavigation.postUserFeedback].
 *
 * This event occurs if the user taps a feedback button in the navigation app indicating there was a problem.
 */
object FeedbackEvent {
    /**
     * Feedback type *general*: when no one else is not fit or actually *general* feedback
     */
    const val GENERAL_ISSUE = "general"

    /**
     * Feedback type *accident*: crash, uneven surface, etc
     */
    const val ACCIDENT = "accident"

    /**
     * Feedback type *hazard*: such as debris, stopped vehicles, etc
     */
    const val HAZARD = "hazard"

    /**
     * Feedback type *road closed*: closed road or one that does not allow vehicles
     */
    const val ROAD_CLOSED = "road_closed"

    /**
     * Feedback type *not allowed*: a turn/maneuver that isn't allowed
     */
    const val NOT_ALLOWED = "not_allowed"

    /**
     * Feedback type *routing error*: poor instruction or route choice
     * (ambiguous or poorly-timed turn announcement, or a set of confusing turns)
     */
    const val ROUTING_ERROR = "routing_error"

    /**
     * Feedback type *missing road*: road doesn't exist
     */
    const val MISSING_ROAD = "missing_road"

    /**
     * Feedback type *missing exit*: dead end
     */
    const val MISSING_EXIT = "missing_exit"

    /**
     * Feedback type *confusing instruction*: wrong voice/text instruction
     */
    const val CONFUSING_INSTRUCTION = "confusing_instruction"

    /**
     * Feedback type *inaccurate gps*: wrong location of navigation puck on the map
     */
    const val INACCURATE_GPS = "inaccurate_gps"

    /**
     * Feedback type *looks incorrect*: wrong visual guidance
     */
    const val INCORRECT_VISUAL_GUIDANCE = "incorrect_visual_guidance"

    /**
     * Feedback type *incorrect audio guidance*: wrong audio guidance
     */
    const val INCORRECT_AUDIO_GUIDANCE = "incorrect_audio_guidance"

    /**
     * Feedback type *positioning issue*: wrong positioning
     */
    const val POSITIONING_ISSUE = "positioning_issue"

    /**
     * Feedback type *arrival information*: user's feelings about the arrival experience
     * as the device comes to the final destination
     */
    const val ARRIVAL_FEEDBACK = "arrival_feedback"

    /**
     * Feedback source *reroute*: the user tapped a feedback button in response to a reroute
     */
    const val REROUTE = "reroute"

    /**
     * Feedback source *user*: the user tapped a feedback button
     */
    const val UI = "user"

    /**
     * Feedback description for *looks incorrect*: turn icon incorrect
     */
    const val TURN_ICON_INCORRECT = "turn_icon_incorrect"

    /**
     * Feedback description for *looks incorrect*: street name incorrect
     */
    const val STREET_NAME_INCORRECT = "street_name_incorrect"

    /**
     * Feedback description for *looks incorrect*: instruction unnecessary
     */
    const val INSTRUCTION_UNNECESSARY = "instruction_unnecessary"

    /**
     * Feedback description for *looks incorrect*: instruction missing
     */
    const val INSTRUCTION_MISSING = "instruction_missing"

    /**
     * Feedback description for *looks incorrect*: maneuver incorrect
     */
    const val MANEUVER_INCORRECT = "maneuver_incorrect"

    /**
     * Feedback description for *looks incorrect*: exit info incorrect
     */
    const val EXIT_INFO_INCORRECT = "exit_info_incorrect"

    /**
     * Feedback description for *looks incorrect*: lane guidance incorrect
     */
    const val LANE_GUIDANCE_INCORRECT = "lane_guidance_incorrect"

    /**
     * Feedback description for *looks incorrect*: road known by different name
     */
    const val ROAD_KNOW_BY_DIFFERENT_NAME = "road_know_by_different_name"

    /**
     * Feedback description for *incorrect audio guidance*: guidance too early
     */
    const val GUIDANCE_TOO_EARLY = "guidance_too_early"

    /**
     * Feedback description for *incorrect audio guidance*: guidance too late
     */
    const val GUIDANCE_TOO_LATE = "guidance_too_late"

    /**
     * Feedback description for *incorrect audio guidance*: pronunciation incorrect
     */
    const val PRONUNCIATION_INCORRECT = "pronunciation_incorrect"

    /**
     * Feedback description for *incorrect audio guidance*: road name repeated
     */
    const val ROAD_NAME_REPEATED = "road_name_repeated"

    /**
     * Feedback description for *routing error*: route not drive-able
     */
    const val ROUTE_NOT_DRIVE_ABLE = "route_not_drive_able"

    /**
     * Feedback description for *routing error*: route not preferred
     */
    const val ROUTE_NOT_PREFERRED = "route_not_preferred"

    /**
     * Feedback description for *routing error*: alternative route not expected
     */
    const val ALTERNATIVE_ROUTE_NOT_EXPECTED =
        "alternative_route_not_expected"

    /**
     * Feedback description for *routing error*: route included missing roads
     */
    const val ROUTE_INCLUDED_MISSING_ROADS = "route_included_missing_roads"

    /**
     * Feedback description for *routing error*: route had roads too narrow to pass
     */
    const val ROUTE_HAD_ROADS_TOO_NARROW_TO_PASS =
        "route_had_roads_too_narrow_to_pass"

    /**
     * Feedback description for *routing error*: routed down a one-way
     */
    const val ROUTED_DOWN_A_ONE_WAY = "routed_down_a_one_way"

    /**
     * Feedback description for *routing error*: turn was not allowed
     */
    const val TURN_WAS_NOT_ALLOWED = "turn_was_not_allowed"

    /**
     * Feedback description for *routing error*: cars not allowed on street
     */
    const val CARS_NOT_ALLOWED_ON_STREET = "cars_not_allowed_on_street"

    /**
     * Feedback description for *routing error*: turn at intersection was unprotected
     */
    const val TURN_AT_INTERSECTION_WAS_UNPROTECTED =
        "turn_at_intersection_was_unprotected"

    /**
     * Feedback description for *routing error*: street permanently blocked off
     */
    const val STREET_PERMANENTLY_BLOCKED_OFF =
        "street_permanently_blocked_off"

    /**
     * Feedback description for *routing error*: road is missing from map
     */
    const val ROAD_IS_MISSING_FROM_MAP = "road_is_missing_from_map"

    /**
     * Type of feedback mean WHAT happen
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        GENERAL_ISSUE,
        ACCIDENT,
        HAZARD,
        ROAD_CLOSED,
        NOT_ALLOWED,
        ROUTING_ERROR,
        MISSING_ROAD,
        MISSING_EXIT,
        CONFUSING_INSTRUCTION,
        INACCURATE_GPS,
        INCORRECT_VISUAL_GUIDANCE,
        INCORRECT_AUDIO_GUIDANCE,
        POSITIONING_ISSUE,
        ARRIVAL_FEEDBACK
    )
    annotation class Type

    /**
     * Feedback source mean WHERE happen
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        REROUTE,
        UI
    )
    annotation class Source

    /**
     * Detail description for different feedback type
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        TURN_ICON_INCORRECT,
        STREET_NAME_INCORRECT,
        INSTRUCTION_UNNECESSARY,
        INSTRUCTION_MISSING,
        MANEUVER_INCORRECT,
        EXIT_INFO_INCORRECT,
        LANE_GUIDANCE_INCORRECT,
        ROAD_KNOW_BY_DIFFERENT_NAME,
        GUIDANCE_TOO_EARLY,
        GUIDANCE_TOO_LATE,
        PRONUNCIATION_INCORRECT,
        ROAD_NAME_REPEATED,
        ROUTE_NOT_DRIVE_ABLE,
        ROUTE_NOT_PREFERRED,
        ALTERNATIVE_ROUTE_NOT_EXPECTED,
        ROUTE_INCLUDED_MISSING_ROADS,
        ROUTE_HAD_ROADS_TOO_NARROW_TO_PASS,
        ROUTED_DOWN_A_ONE_WAY,
        TURN_WAS_NOT_ALLOWED,
        CARS_NOT_ALLOWED_ON_STREET,
        TURN_AT_INTERSECTION_WAS_UNPROTECTED,
        STREET_PERMANENTLY_BLOCKED_OFF,
        ROAD_IS_MISSING_FROM_MAP
    )
    annotation class Description
}
