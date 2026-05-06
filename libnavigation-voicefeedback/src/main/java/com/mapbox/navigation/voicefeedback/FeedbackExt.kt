package com.mapbox.navigation.voicefeedback

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.postUserFeedback
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper

/**
 * Send user voice feedback about an issue or problem with the Navigation SDK.
 *
 * Method can be invoked out of the trip session
 * (whenever until [MapboxNavigation.onDestroy] is called),
 * because a feedback is attached to passed location and time in the past
 * when [FeedbackMetadata] was generated (see [MapboxNavigation.provideFeedbackMetadataWrapper]).
 *
 * @param feedbackSubType feedback subtype [FeedbackEvent.SubType] or custom feedback subtype
 * @param description description message
 * @param screenshot encoded screenshot
 * @param feedbackMetadata use it to attach feedback to a specific passed location.
 * See [FeedbackMetadata] and [FeedbackMetadataWrapper]
 * @param userFeedbackCallback invoked when the posted feedback has been processed
 *
 * to retrieve possible feedback subtypes for a given [feedbackSubType]
 * @see [com.mapbox.navigation.ui.maps.util.ViewUtils.capture] to capture screenshots
 * @see [FeedbackHelper.encodeScreenshot] to encode screenshots
 */
@ExperimentalPreviewMapboxNavigationAPI
@JvmOverloads
fun MapboxNavigation.postVoiceFeedback(
    @FeedbackEvent.SubType feedbackSubType: String,
    description: String,
    screenshot: String,
    feedbackMetadata: FeedbackMetadata? = null,
    userFeedbackCallback: UserFeedbackCallback,
) {
    postUserFeedback(
        feedbackType = FeedbackEvent.VOICE_FEEDBACK,
        description = description,
        feedbackSource = FeedbackEvent.UI,
        screenshot = screenshot,
        feedbackSubType = arrayOf(feedbackSubType),
        feedbackMetadata = feedbackMetadata,
        userFeedbackCallback = userFeedbackCallback,
    )
}

/**
 * Feedback source *voice*: the user tapped a voice feedback button and send a message
 */
private val FeedbackEvent.VOICE_FEEDBACK: String get() = "voice_feedback"
