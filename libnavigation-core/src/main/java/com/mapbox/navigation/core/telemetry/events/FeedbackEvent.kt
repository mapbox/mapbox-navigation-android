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
         * Feedback source *reroute*: the user tapped a feedback button in response to a reroute
         */
        const val FEEDBACK_SOURCE_REROUTE = "reroute"

        /**
         * Feedback source *user*: the user tapped a feedback button
         */
        const val FEEDBACK_SOURCE_UI = "user"
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
}
