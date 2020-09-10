package com.mapbox.navigation.ui.feedback

import android.content.Context
import androidx.annotation.DrawableRes
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.ui.R

/**
 * A helper to get feedback text and image resource by [FeedbackEvent.Type]
 */
internal object FeedbackHelper {
    fun getFeedbackText(@FeedbackEvent.Type feedbackType: String, context: Context): String {
        val textResource =
            when (feedbackType) {
                FeedbackEvent.INCORRECT_VISUAL_GUIDANCE ->
                    R.string.mapbox_feedback_type_looks_incorrect
                FeedbackEvent.INCORRECT_AUDIO_GUIDANCE ->
                    R.string.mapbox_feedback_type_confusing_audio
                FeedbackEvent.POSITIONING_ISSUE ->
                    R.string.mapbox_feedback_type_positioning_issue
                FeedbackEvent.ROUTING_ERROR ->
                    R.string.mapbox_feedback_type_route_quality
                FeedbackEvent.NOT_ALLOWED ->
                    R.string.mapbox_feedback_type_illegal_route
                FeedbackEvent.ROAD_CLOSED ->
                    R.string.mapbox_feedback_type_road_closure
                else -> throw IllegalArgumentException(
                    "feedback type $feedbackType is not supported"
                )
            }

        return context.resources.getString(textResource)
    }

    @DrawableRes
    fun getFeedbackImage(@FeedbackEvent.Type feedbackType: String): Int {
        return when (feedbackType) {
            FeedbackEvent.INCORRECT_VISUAL_GUIDANCE ->
                R.drawable.mapbox_ic_feedback_looks_incorrect
            FeedbackEvent.INCORRECT_AUDIO_GUIDANCE ->
                R.drawable.mapbox_ic_feedback_confusing_audio
            FeedbackEvent.POSITIONING_ISSUE ->
                R.drawable.mapbox_ic_feedback_positioning_issue
            FeedbackEvent.ROUTING_ERROR ->
                R.drawable.mapbox_ic_feedback_route_quality
            FeedbackEvent.NOT_ALLOWED ->
                R.drawable.mapbox_ic_feedback_illegal_route
            FeedbackEvent.ROAD_CLOSED ->
                R.drawable.mapbox_ic_feedback_road_closure
            else -> throw IllegalArgumentException("feedback type $feedbackType is not supported")
        }
    }
}
