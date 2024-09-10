package com.mapbox.navigation.core.telemetry.events

import android.graphics.Bitmap
import android.util.Base64
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.utils.encode

/**
 * Provides utilities for working with feedback
 */
object FeedbackHelper {

    /**
     * Returns feedback types to send during Free Drive.
     *
     * @return array of free drive feedback types
     */
    @JvmStatic
    fun getFreeDriveFeedbackTypes(): Array<@FeedbackEvent.Type String> {
        return arrayOf(
            FeedbackEvent.INCORRECT_VISUAL,
            FeedbackEvent.ROAD_ISSUE,
            FeedbackEvent.TRAFFIC_ISSUE,
            FeedbackEvent.POSITIONING_ISSUE,
            FeedbackEvent.OTHER_ISSUE,
        )
    }

    /**
     * Returns feedback types to send during Active Navigation.
     *
     * @return array of active navigation feedback types
     */
    @JvmStatic
    fun getActiveNavigationFeedbackTypes(): Array<@FeedbackEvent.Type String> {
        return arrayOf(
            FeedbackEvent.INCORRECT_VISUAL_GUIDANCE,
            FeedbackEvent.INCORRECT_AUDIO_GUIDANCE,
            FeedbackEvent.ROUTING_ERROR,
            FeedbackEvent.ROUTE_NOT_ALLOWED,
            FeedbackEvent.ROAD_CLOSED,
            FeedbackEvent.POSITIONING_ISSUE,
        )
    }

    /**
     * Returns feedback types to send upon arrival at the destination point.
     *
     * @return array of arrival feedback types
     */
    @JvmStatic
    fun getArrivalFeedbackTypes(): Array<@FeedbackEvent.Type String> {
        return arrayOf(
            FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
            FeedbackEvent.ARRIVAL_FEEDBACK_NOT_GOOD,
        )
    }

    /**
     * Returns feedback subtypes that can be sent with a given [feedbackType].
     *
     * @param feedbackType feedback type to send
     * @return array of feedback subtypes for a given [feedbackType], may be empty
     * @throws IllegalArgumentException if [feedbackType] is unknown
     */
    @JvmStatic
    fun getFeedbackSubTypes(
        @FeedbackEvent.Type feedbackType: String,
    ): Array<@FeedbackEvent.SubType String> {
        return when (feedbackType) {
            FeedbackEvent.INCORRECT_VISUAL -> arrayOf(
                FeedbackEvent.STREET_NAME_INCORRECT,
                FeedbackEvent.INCORRECT_SPEED_LIMIT,
            )
            FeedbackEvent.ROAD_ISSUE -> arrayOf(
                FeedbackEvent.STREET_PERMANENTLY_BLOCKED_OFF,
                FeedbackEvent.STREET_TEMPORARILY_BLOCKED_OFF,
                FeedbackEvent.MISSING_ROAD,
            )
            FeedbackEvent.TRAFFIC_ISSUE -> arrayOf(
                FeedbackEvent.TRAFFIC_CONGESTION,
                FeedbackEvent.TRAFFIC_MODERATE,
                FeedbackEvent.TRAFFIC_NO,
            )
            FeedbackEvent.POSITIONING_ISSUE -> emptyArray()
            FeedbackEvent.OTHER_ISSUE -> emptyArray()
            FeedbackEvent.INCORRECT_VISUAL_GUIDANCE -> arrayOf(
                FeedbackEvent.TURN_ICON_INCORRECT,
                FeedbackEvent.STREET_NAME_INCORRECT,
                FeedbackEvent.INSTRUCTION_UNNECESSARY,
                FeedbackEvent.INSTRUCTION_MISSING,
                FeedbackEvent.MANEUVER_INCORRECT,
                FeedbackEvent.EXIT_INFO_INCORRECT,
                FeedbackEvent.LANE_GUIDANCE_INCORRECT,
                FeedbackEvent.INCORRECT_SPEED_LIMIT,
            )
            FeedbackEvent.INCORRECT_AUDIO_GUIDANCE -> arrayOf(
                FeedbackEvent.GUIDANCE_TOO_EARLY,
                FeedbackEvent.GUIDANCE_TOO_LATE,
                FeedbackEvent.PRONUNCIATION_INCORRECT,
                FeedbackEvent.ROAD_NAME_REPEATED,
                FeedbackEvent.INSTRUCTION_MISSING,
            )
            FeedbackEvent.ROUTING_ERROR -> arrayOf(
                FeedbackEvent.ROUTE_NOT_DRIVE_ABLE,
                FeedbackEvent.ROUTE_NOT_PREFERRED,
                FeedbackEvent.ALTERNATIVE_ROUTE_NOT_EXPECTED,
                FeedbackEvent.ROUTE_INCLUDED_MISSING_ROADS,
                FeedbackEvent.ROUTE_HAD_ROADS_TOO_NARROW_TO_PASS,
            )
            FeedbackEvent.ROUTE_NOT_ALLOWED -> arrayOf(
                FeedbackEvent.ROUTED_DOWN_A_ONE_WAY,
                FeedbackEvent.TURN_WAS_NOT_ALLOWED,
                FeedbackEvent.CARS_NOT_ALLOWED_ON_STREET,
            )
            FeedbackEvent.ROAD_CLOSED -> arrayOf(
                FeedbackEvent.STREET_PERMANENTLY_BLOCKED_OFF,
            )
            FeedbackEvent.ARRIVAL_FEEDBACK_GOOD -> emptyArray()
            FeedbackEvent.ARRIVAL_FEEDBACK_NOT_GOOD -> arrayOf(
                FeedbackEvent.ARRIVAL_FEEDBACK_WRONG_LOCATION,
                FeedbackEvent.ARRIVAL_FEEDBACK_WRONG_ENTRANCE,
                FeedbackEvent.ARRIVAL_FEEDBACK_CONFUSING_INSTRUCTIONS,
                FeedbackEvent.ARRIVAL_FEEDBACK_THIS_PLACE_IS_CLOSED,
            )
            else -> throw IllegalArgumentException("feedback type $feedbackType is not supported")
        }
    }

    /**
     * Scales and compresses a [Bitmap], then encodes the result to a [Base64] string.
     * Returned [String] can be passed to [MapboxNavigation.postUserFeedback] function.
     *
     * @return encoded screenshot as a [Base64] string
     */
    @JvmStatic
    @JvmOverloads
    fun encodeScreenshot(
        screenshot: Bitmap,
        options: BitmapEncodeOptions = BitmapEncodeOptions.Builder().build(),
    ): String {
        return Base64.encodeToString(screenshot.encode(options), Base64.DEFAULT)
    }
}
