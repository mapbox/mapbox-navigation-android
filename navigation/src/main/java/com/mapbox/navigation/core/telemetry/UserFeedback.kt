package com.mapbox.navigation.core.telemetry

import android.graphics.Bitmap
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.ExtendedUserFeedback
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigator.ScreenshotFormat

/**
 * Class for user feedbacks, contains properties that were passed to
 * [MapboxNavigation.postUserFeedback], feedback id and location at that moment.
 *
 * @property feedbackType feedback type, one of [FeedbackEvent.Type] or a custom one
 * @property feedbackSubTypes list of [FeedbackEvent.SubType] and/or custom feedback subtypes
 * @property description description message
 * @property screenshot base64-encoded screenshot that will be attached to feedback
 * @property feedbackMetadata use it to attach feedback to a specific passed location.
 */
@ExperimentalPreviewMapboxNavigationAPI
class UserFeedback private constructor(
    @FeedbackEvent.Type val feedbackType: String,
    val feedbackSubTypes: List<String>,
    val description: String,
    val screenshot: String?,
    val feedbackMetadata: FeedbackMetadata?,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder(feedbackType, description)
        .feedbackSubTypes(feedbackSubTypes)
        .screenshot(screenshot)
        .feedbackMetadata(feedbackMetadata)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserFeedback

        if (feedbackType != other.feedbackType) return false
        if (feedbackSubTypes != other.feedbackSubTypes) return false
        if (description != other.description) return false
        if (screenshot != other.screenshot) return false
        if (feedbackMetadata != other.feedbackMetadata) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = feedbackType.hashCode()
        result = 31 * result + feedbackSubTypes.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (screenshot?.hashCode() ?: 0)
        result = 31 * result + (feedbackMetadata?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "UserFeedback(" +
            "feedbackType='$feedbackType', " +
            "feedbackSubTypes=$feedbackSubTypes, " +
            "description='$description', " +
            "screenshot=$screenshot, " +
            "feedbackMetadata=$feedbackMetadata" +
            ")"
    }

    /**
     * Builder for [UserFeedback].
     *
     * @property feedbackType feedback type, one of [FeedbackEvent.Type] or a custom one
     * @property description description message
     */
    class Builder(
        @FeedbackEvent.Type private val feedbackType: String,
        private val description: String,
    ) {

        private var screenshot: String? = null
        private var feedbackMetadata: FeedbackMetadata? = null
        private var feedbackSubTypes: List<String> = emptyList()

        /**
         * List of [FeedbackEvent.SubType] and/or custom feedback subtypes
         */
        fun feedbackSubTypes(feedbackSubTypes: List<String>): Builder = apply {
            this.feedbackSubTypes = feedbackSubTypes
        }

        /**
         * Screenshot that will be attached to feedback
         */
        fun screenshot(screenshot: Bitmap): Builder = apply {
            this.screenshot = FeedbackHelper.encodeScreenshot(screenshot)
        }

        /**
         * Base64-encoded screenshot that will be attached to feedback
         */
        fun screenshot(screenshot: String?): Builder = apply {
            this.screenshot = screenshot
        }

        /**
         * Use it to attach feedback to a specific passed location
         */
        fun feedbackMetadata(feedbackMetadata: FeedbackMetadata?): Builder = apply {
            this.feedbackMetadata = feedbackMetadata
        }

        /**
         * Build the [UserFeedback]
         */
        fun build() = UserFeedback(
            feedbackType = feedbackType,
            feedbackSubTypes = feedbackSubTypes,
            description = description,
            screenshot = screenshot,
            feedbackMetadata = feedbackMetadata,
        )
    }

    internal companion object {

        @JvmSynthetic
        fun UserFeedback.mapToNative(): com.mapbox.navigator.UserFeedback {
            return com.mapbox.navigator.UserFeedback(
                feedbackType,
                feedbackSubTypes,
                description,
                ScreenshotFormat(null, screenshot),
            )
        }

        @JvmSynthetic
        fun UserFeedback.mapToInternal(
            feedbackId: String,
            location: Point,
        ): ExtendedUserFeedback {
            return ExtendedUserFeedback(
                feedback = this,
                feedbackId = feedbackId,
                location = location,
            )
        }
    }
}
