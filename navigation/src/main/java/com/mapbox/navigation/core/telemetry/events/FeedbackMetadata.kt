package com.mapbox.navigation.core.telemetry.events

import com.google.gson.Gson
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.UserFeedbackHandle
import com.mapbox.navigator.UserFeedbackMetadata

/**
 * It's the wrapper of [FeedbackMetadata] that collect locations after instance of
 * [FeedbackMetadataWrapper] is created and stop collecting in 2 possible cases:
 * - locations buffer is full;
 * - [FeedbackMetadataWrapper.get] is called.
 *
 * Notes:
 * - if you need to serialize Feedback metadata, use [FeedbackMetadata] only (see [FeedbackMetadataWrapper.get]);
 * - to collect as much data as possible preferably call [FeedbackMetadataWrapper.get] just before posting or storing feedback.
 */
@ExperimentalPreviewMapboxNavigationAPI
class FeedbackMetadataWrapper private constructor(
    private val userFeedbackHandle: UserFeedbackHandle,
) {

    /**
     * Provides instance of [FeedbackMetadata]
     */
    fun get(): FeedbackMetadata = FeedbackMetadata.create(userFeedbackHandle.metadata)

    internal companion object {

        @JvmSynthetic
        fun create(userFeedbackHandle: UserFeedbackHandle): FeedbackMetadataWrapper {
            return FeedbackMetadataWrapper(userFeedbackHandle)
        }
    }
}

/**
 * Feedback metadata is used as part of [MapboxNavigation.postUserFeedback] to send deferred feedback.
 * It contains data(like session ids, locations before and after call and so on) from a particular
 * point of time/location when [MapboxNavigation.provideFeedbackMetadataWrapper] is called.
 *
 * Note: [MapboxNavigation.provideFeedbackMetadataWrapper] returns wrapper of
 * [FeedbackMetadata] that collect **locations after** call under the hood.
 */
@ExperimentalPreviewMapboxNavigationAPI
class FeedbackMetadata private constructor(
    internal val userFeedbackMetadata: UserFeedbackMetadata,
) {

    companion object {

        private const val LOG_CATEGORY = "FeedbackMetadata"

        @JvmSynthetic
        internal fun create(userFeedbackMetadata: UserFeedbackMetadata): FeedbackMetadata {
            return FeedbackMetadata(userFeedbackMetadata)
        }

        /**
         * Creates a new instance of [FeedbackMetadata] from json.
         * Note that the internal structure of [FeedbackMetadata] can change at any time,
         * so a [FeedbackMetadata] object converted to JSON in older versions of the SDK,
         * might not be restored correctly. In such cases, null is returned.
         */
        @JvmStatic
        @ExperimentalPreviewMapboxNavigationAPI
        fun fromJson(json: String): FeedbackMetadata? =
            try {
                Gson().fromJson(json, FeedbackMetadata::class.java)
            } catch (e: Exception) {
                logE("Unable to create FeedbackMetadata: $e", LOG_CATEGORY)
                null
            }
    }

    /**
     * Serialize [FeedbackMetadata] to json string.
     *
     * Note that the internal structure of [FeedbackMetadata] can change at any time,
     * so a [FeedbackMetadata] object converted to JSON in older versions of the SDK,
     * might not be restored correctly using [fromJson].
     */
    fun toJson(gson: Gson): String = gson.toJson(this)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeedbackMetadata

        return userFeedbackMetadata == other.userFeedbackMetadata
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return userFeedbackMetadata.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "FeedbackMetadata(userFeedbackMetadata=$userFeedbackMetadata)"
    }
}
