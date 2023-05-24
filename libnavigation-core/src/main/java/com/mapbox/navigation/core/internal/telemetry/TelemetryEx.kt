@file:Suppress("unused")

package com.mapbox.navigation.core.internal.telemetry

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.telemetry.events.UserFeedback

/**
 * Register a [UserFeedbackCallback] to be notified when a new user feedback is posted.
 *
 * @param userFeedbackCallback UserFeedbackCallback
 */
fun registerUserFeedbackCallback(
    mapboxNavigation: MapboxNavigation, userFeedbackCallback: UserFeedbackCallback
) {
    mapboxNavigation.userFeedbackSubscriber.registerUserFeedbackCallback(userFeedbackCallback)
}

/**
 * Unregisters a [UserFeedbackCallback].
 *
 * @param userFeedbackCallback UserFeedbackCallback
 */
fun unregisterUserFeedbackCallback(
    mapboxNavigation: MapboxNavigation, userFeedbackCallback: UserFeedbackCallback
) {
    mapboxNavigation.userFeedbackSubscriber.unregisterUserFeedbackCallback(userFeedbackCallback)
}

/**
 * Send user feedback about an issue or problem with the Navigation SDK.
 *
 * Method can be invoked out of the trip session
 * (whenever until [MapboxNavigation.onDestroy] is called),
 * because a feedback is attached to passed location and time in the past
 * when [FeedbackMetadata] was generated (see [MapboxNavigation.provideFeedbackMetadataWrapper]).
 *
 * @param feedbackType one of [FeedbackEvent.Type] or a custom one
 * @param description description message
 * @param feedbackSource one of [FeedbackEvent.Source]
 * @param screenshot encoded screenshot
 * @param feedbackSubType optional array of [FeedbackEvent.SubType] and/or custom feedback subtypes
 * @param feedbackMetadata use it to attach feedback to a specific passed location.
 * See [FeedbackMetadata] and [FeedbackMetadataWrapper]
 * @param userFeedbackCallback invoked when the posted feedback has been processed
 *
 * @see [FeedbackHelper.getFeedbackSubTypes]
 * to retrieve possible feedback subtypes for a given [feedbackType]
 * @see [ViewUtils.capture] to capture screenshots
 * @see [FeedbackHelper.encodeScreenshot] to encode screenshots
 */
@ExperimentalPreviewMapboxNavigationAPI
@JvmOverloads
fun MapboxNavigation.postUserFeedback(
    userFeedback: UserFeedback,
    userFeedbackCallback: UserFeedbackCallback,
) {
    postUserFeedback(
        userFeedback,
        userFeedbackCallback,
    )
}

@ExperimentalPreviewMapboxNavigationAPI
fun MapboxNavigation.sendCustomEvent(
    payload: String,
    @CustomEvent.Type customEventType: String,
    customEventVersion: String,
) {
    postCustomEvent(payload, customEventType, customEventVersion)
}

fun MapboxNavigation.telemetryAndroidAutoInterface(): TelemetryAndroidAutoInterface =
    telemetryAndroidAutoInterface
