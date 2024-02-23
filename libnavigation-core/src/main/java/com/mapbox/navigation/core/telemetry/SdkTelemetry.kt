package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.CustomEvent
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper

internal interface SdkTelemetry {

    fun postCustomEvent(
        payload: String,
        @CustomEvent.Type customEventType: String,
        customEventVersion: String,
    )

    @ExperimentalPreviewMapboxNavigationAPI
    fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper?

    @ExperimentalPreviewMapboxNavigationAPI
    fun postUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
        feedbackMetadata: FeedbackMetadata?,
        userFeedbackCallback: UserFeedbackCallback?,
    )

    fun destroy(mapboxNavigation: MapboxNavigation)

    companion object {

        val EMPTY = object : SdkTelemetry {
            override fun postCustomEvent(
                payload: String,
                customEventType: String,
                customEventVersion: String,
            ) {
                // do nothing
            }

            @ExperimentalPreviewMapboxNavigationAPI
            override fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper? = null

            @ExperimentalPreviewMapboxNavigationAPI
            override fun postUserFeedback(
                feedbackType: String,
                description: String,
                feedbackSource: String,
                screenshot: String?,
                feedbackSubType: Array<String>?,
                feedbackMetadata: FeedbackMetadata?,
                userFeedbackCallback: UserFeedbackCallback?,
            ) {
                // do nothing
            }

            override fun destroy(mapboxNavigation: MapboxNavigation) {
                // do nothing
            }
        }
    }
}
