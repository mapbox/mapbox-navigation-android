package com.mapbox.navigation.core.telemetry.events

import android.graphics.Bitmap
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackInternal
import com.mapbox.navigation.core.telemetry.events.UserFeedback.Companion.toNative
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class UserFeedback private constructor(
    val feedbackType: String,
    val description: String,
    val feedbackSubTypes: Array<String>,
    val screenshot: Bitmap?,
    val feedbackMetadata: FeedbackMetadata? = null
) {

    internal companion object {
        internal fun UserFeedback.toNative(): com.mapbox.navigator.UserFeedback =
            com.mapbox.navigator.UserFeedback(
                feedbackType,
                feedbackSubTypes.toList(),
                "",
                description,
                screenshot?.let { bitmap ->
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    val byteBuffer = ByteBuffer.allocateDirect(out.size())
                    byteBuffer.put(out.toByteArray())
                    return@let DataRef(byteBuffer)
                }
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserFeedback

        if (feedbackType != other.feedbackType) return false
        if (description != other.description) return false
        if (!feedbackSubTypes.contentEquals(other.feedbackSubTypes)) return false
        if (screenshot != other.screenshot) return false
        if (feedbackMetadata != other.feedbackMetadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = feedbackType.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + feedbackSubTypes.contentHashCode()
        result = 31 * result + screenshot.hashCode()
        result = 31 * result + feedbackMetadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "UserFeedback(" +
            "feedbackType='$feedbackType', " +
            "description='$description', " +
            "feedbackSubType=${feedbackSubTypes.contentToString()}, " +
            "screenshot=$screenshot" +
            "feedbackMetadata=$feedbackMetadata" +
            ")"
    }

    /**
     * Builder [UserFeedback]
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    class Builder(
        private val feedbackType: String,
        private val description: String,
    ) {
        private var screenshot: Bitmap? = null
        private var feedbackSubTypes: Array<String> = emptyArray()
        private var feedbackMetadata: FeedbackMetadata? = null

        fun screenshot(screenshot: Bitmap) = apply {
            this.screenshot = screenshot
        }

        fun feedbackSubTypes(feedbackSubTypes: Array<String>) = apply {
            this.feedbackSubTypes = feedbackSubTypes
        }

        @ExperimentalPreviewMapboxNavigationAPI
        fun feedbackMetadata(feedbackMetadata: FeedbackMetadata) = apply {
            this.feedbackMetadata = feedbackMetadata
        }

        fun build(): UserFeedback = UserFeedback(
            feedbackType,
            description,
            feedbackSubTypes,
            screenshot,
            feedbackMetadata,
        )
    }
}
