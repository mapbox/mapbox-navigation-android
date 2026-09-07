package com.mapbox.navigation.ui.androidauto.feedback.core

import androidx.annotation.Keep
import androidx.annotation.UiThread
import com.google.gson.Gson
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackItem

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class CarFeedbackSender {

    private val gson = Gson()

    @UiThread
    fun send(
        selectedItem: CarFeedbackItem,
        encodedSnapshot: String?,
        sourceScreenSimpleName: String,
    ) {
        val mapboxNavigation = MapboxNavigationApp.current()
        val feedbackMetadata = mapboxNavigation?.provideFeedbackMetadataWrapper()?.get()

        if (selectedItem.navigationFeedbackType != null && feedbackMetadata != null) {
            mapboxNavigation.postUserFeedback(
                feedbackType = selectedItem.navigationFeedbackType,
                description = "Android Auto selection: ${selectedItem.carFeedbackTitle}",
                feedbackSource = FeedbackEvent.UI,
                screenshot = encodedSnapshot.orEmpty(),
                feedbackSubType = selectedItem.navigationFeedbackSubType?.toTypedArray(),
                feedbackMetadata = feedbackMetadata,
            )
        }

        // TODO send search feedback
//        ifNonNull(selectedItem?.searchFeedbackReason) { _ ->
//            val analyticsService = MapboxSearchSdk.serviceProvider.analyticsService()
//            analyticsService.sendRawFeedbackEvent()
//        }

        // Collect feedback in the history recorder.
        recordFeedbackInHistory(
            CarFeedbackHistoryEvent(
                sourceScreen = sourceScreenSimpleName,
                carFeedbackItem = selectedItem,
                encodedSnapshot = encodedSnapshot,
                feedbackMetadata = feedbackMetadata,
            ),
        )
    }

    private fun recordFeedbackInHistory(historyEvent: CarFeedbackHistoryEvent) {
        val eventJson = historyEvent.run { gson.toJson(historyEvent) } ?: ""
        check(eventJson != "null") {
            "The event did not car event to json: $eventJson $eventJson"
        }
        MapboxNavigationApp.current()?.historyRecorder
            ?.pushHistory("car_feedback_sent", eventJson)
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Keep
internal data class CarFeedbackHistoryEvent(
    val sourceScreen: String,
    val carFeedbackItem: CarFeedbackItem? = null,
    val encodedSnapshot: String? = null,
    val feedbackMetadata: FeedbackMetadata? = null,
)
