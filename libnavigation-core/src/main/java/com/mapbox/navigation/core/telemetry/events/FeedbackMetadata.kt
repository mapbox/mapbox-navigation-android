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
class FeedbackMetadataWrapper internal constructor(
    private val userFeedbackHandle: UserFeedbackHandle
) {
    fun get(): FeedbackMetadata = FeedbackMetadata(userFeedbackHandle.metadata)
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
class FeedbackMetadata internal constructor(
    internal val userFeedbackMetadata: UserFeedbackMetadata,
) {

    companion object {
        private const val LOG_CATEGORY = "FeedbackMetadata"

        /**
         * Create a new instance of [FeedbackMetadata] from json.
         */
        @JvmStatic
        @ExperimentalPreviewMapboxNavigationAPI
        fun fromJson(json: String): FeedbackMetadata? =
            try {
                Gson().fromJson(json, FeedbackMetadata::class.java)
            } catch (e: Exception) {
                logE("from json exception: $e", LOG_CATEGORY)
                null
            }
    }

    /**
     * Serialize [FeedbackMetadata] to json string.
     */
    fun toJson(gson: Gson): String = gson.toJson(this)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeedbackMetadata

        if (userFeedbackMetadata != other.userFeedbackMetadata) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return userFeedbackMetadata.hashCode()
    }
}
