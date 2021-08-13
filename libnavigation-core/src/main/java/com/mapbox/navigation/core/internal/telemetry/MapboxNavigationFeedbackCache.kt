package com.mapbox.navigation.core.internal.telemetry

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.MapboxNavigationFeedbackCache.cacheUserFeedback
import com.mapbox.navigation.core.internal.telemetry.MapboxNavigationFeedbackCache.getCachedUserFeedback
import com.mapbox.navigation.core.internal.telemetry.MapboxNavigationFeedbackCache.postCachedUserFeedback
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent

/**
 * This object is only intent to be used internally and subject to change in future.
 *
 * This object is only a wrapper of [MapboxNavigationTelemetry] to cache/retrieve/post a navigation.feedback event.
 * It must be used after [MapboxNavigationTelemetry.initialize] has been called.
 *
 * A complete feedback cache flow will be:
 * 1. [cacheUserFeedback] to generate a navigation.feedback event in [MapboxNavigationTelemetry] internally.
 * 2. [getCachedUserFeedback] to retrieve all cached navigation.feedback events from [MapboxNavigationTelemetry].
 * 3. make possible changes to the list that is returned by [getCachedUserFeedback].
 * 4. submit changed list from step 3 to [postCachedUserFeedback] to [MapboxNavigationTelemetry].
 *
 * Note:
 * 1. Only the feedback events created by [cacheUserFeedback] and returned by [getCachedUserFeedback]
 * are valid in [postCachedUserFeedback]. All user manually created events will be dropped.
 * 2. The cache APIs should only be used when in Active Guidance mode since navigation.feedback events are only handled in that mode.
 * 3. [postCachedUserFeedback] should be called before transitioning between modes. Otherwise, all cached events will be lost.
 */
object MapboxNavigationFeedbackCache {

    /**
     * Create and cache a user feedback about an issue or problem with the Navigation SDK.
     *
     * Instead of sending the feedback, this API will cache the feedback in the Navigation SDK. The cached feedbacks can be retrieved by calling [getCachedUserFeedback] so the user can update and send them in a later time. To send the cached feedbacks, please use [postCachedUserFeedback].
     *
     * If you want to send the feedback directly without caching it, you should use [MapboxNavigation.postUserFeedback].
     *
     * @param feedbackType one of [FeedbackEvent.Type]
     * @param description description message
     * @param feedbackSource one of [FeedbackEvent.Source]
     * @param screenshot encoded screenshot (optional)
     * @param feedbackSubType array of [FeedbackEvent.Description] (optional)
     *
     * @see [MapboxNavigation.postUserFeedback]
     * @see [getCachedUserFeedback]
     * @see [postCachedUserFeedback]
     */
    fun cacheUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>? = emptyArray(),
    ) {
        MapboxNavigationTelemetry.cacheUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
        )
    }

    /**
     * Get a list of [CachedNavigationFeedbackEvent] which was created by [cacheUserFeedback].
     *
     * You can update the cached feedback to have more information like [CachedNavigationFeedbackEvent.feedbackSubType] and [CachedNavigationFeedbackEvent.description] or other fields. And use [postCachedUserFeedback] to send the updated cached feedbacks.
     *
     * @see [cacheUserFeedback]
     * @see [postCachedUserFeedback]
     *
     * @return a list of [CachedNavigationFeedbackEvent]s
     */
    fun getCachedUserFeedback(): List<CachedNavigationFeedbackEvent> {
        return MapboxNavigationTelemetry.getCachedUserFeedback()
    }

    /**
     * Send a list of [CachedNavigationFeedbackEvent]s.
     *
     * Only the feedback that's been created by [cacheUserFeedback] will be sent. Otherwise that feedback will be dropped and not be sent.
     *
     * A complete caching feedback flow looks like this:
     * 1. Create and cache feedback by calling [cacheUserFeedback].
     * 2. Retrieve the cached feedback list by calling [getCachedUserFeedback].
     * 3. Update the cached feedback list to provide more information of the feedback.
     * 4. Call [postCachedUserFeedback] to send the updated cached feedback list.
     *
     * Note:
     * 1. Only the feedback events created by [cacheUserFeedback] and returned by [getCachedUserFeedback]
     * are valid in [postCachedUserFeedback]. All user manually created events will be dropped.
     * 2. The cache APIs should only be used when in Active Guidance mode since navigation.feedback events are only handled in that mode.
     * 3. [postCachedUserFeedback] should be called before transitioning between modes. Otherwise, all cached events will be lost.
     *
     * @param cachedFeedbackEventList the list should be the subset or the original list that you get from [getCachedUserFeedback].
     *
     * @see [cacheUserFeedback]
     * @see [getCachedUserFeedback]
     */
    fun postCachedUserFeedback(cachedFeedbackEventList: List<CachedNavigationFeedbackEvent>) {
        MapboxNavigationTelemetry.postCachedUserFeedback(cachedFeedbackEventList)
    }
}
