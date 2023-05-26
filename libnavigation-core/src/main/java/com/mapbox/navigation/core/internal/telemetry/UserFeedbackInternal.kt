package com.mapbox.navigation.core.internal.telemetry

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.UserFeedback

/**
 * Class for user feedbacks, contains properties that were passed to
 * [MapboxNavigation.postUserFeedback], feedback id and location at that moment.
 *
 * @property feedbackId this feedback's unique identifier
 * @property feedbackType feedback type, one of [FeedbackEvent.Type] or a custom one
 * @property source one of [FeedbackEvent.Source]
 * @property description description message
 * @property screenshot encoded screenshot
 * @property feedbackSubType array of [FeedbackEvent.SubType] and/or custom feedback subtypes
 * @property location user location when the feedback event was posted
 */
class UserFeedbackInternal internal constructor(
    val feedbackId: String,
    val feedbackType: String,
    val description: String,
    val feedbackSubType: Array<String>?,
    val location: Point,
) {

    companion object {
        fun UserFeedback.toInternal(
            feedbackId: String,
            location: Point,
        ): UserFeedbackInternal = UserFeedbackInternal(
            feedbackId = feedbackId,
            feedbackType = feedbackType,
            description = description,
            feedbackSubType = feedbackSubTypes,
            location = location,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserFeedbackInternal

        if (feedbackId != other.feedbackId) return false
        if (feedbackType != other.feedbackType) return false
        if (description != other.description) return false
        if (!feedbackSubType.contentEquals(other.feedbackSubType)) return false
        if (location != other.location) return false

        return true
    }

    override fun hashCode(): Int {
        var result = feedbackId.hashCode()
        result = 31 * result + feedbackType.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + feedbackSubType.contentHashCode()
        result = 31 * result + location.hashCode()
        return result
    }

    override fun toString(): String {
        return "UserFeedback(" +
            "feedbackId='$feedbackId', " +
            "feedbackType='$feedbackType', " +
            "description='$description', " +
            "feedbackSubType=${feedbackSubType.contentToString()}, " +
            "location=$location" +
            ")"
    }
}
