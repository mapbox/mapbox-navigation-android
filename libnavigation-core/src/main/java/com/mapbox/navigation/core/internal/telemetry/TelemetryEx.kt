@file:Suppress("unused")

package com.mapbox.navigation.core.internal.telemetry

import android.location.Location
import android.os.Build
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.telemetry.events.TelemetryLocation

internal fun List<Location>.toTelemetryLocations(): Array<TelemetryLocation> {
    return Array(size) { get(it).toTelemetryLocation() }
}

internal fun Location.toTelemetryLocation(): TelemetryLocation {
    return TelemetryLocation(
        latitude,
        longitude,
        speed,
        bearing,
        altitude,
        time.toString(),
        accuracy,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verticalAccuracyMeters
        } else {
            0f
        },
    )
}

/**
 * Register a [UserFeedbackCallback] to be notified when a new user feedback is posted.
 *
 * @param userFeedbackCallback UserFeedbackCallback
 */
fun registerUserFeedbackCallback(userFeedbackCallback: UserFeedbackCallback) {
    MapboxNavigationTelemetry.registerUserFeedbackCallback(userFeedbackCallback)
}

/**
 * Unregisters a [UserFeedbackCallback].
 *
 * @param userFeedbackCallback UserFeedbackCallback
 */
fun unregisterUserFeedbackCallback(userFeedbackCallback: UserFeedbackCallback) {
    MapboxNavigationTelemetry.unregisterUserFeedbackCallback(userFeedbackCallback)
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
    feedbackType: String,
    description: String,
    @FeedbackEvent.Source feedbackSource: String,
    screenshot: String,
    feedbackSubType: Array<String>? = emptyArray(),
    feedbackMetadata: FeedbackMetadata? = null,
    userFeedbackCallback: UserFeedbackCallback,
) {
    postUserFeedback(
        feedbackType, description, feedbackSource, screenshot,
        feedbackSubType, feedbackMetadata, userFeedbackCallback,
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
