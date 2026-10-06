package com.mapbox.navigation.ui.androidauto.feedback.core

import androidx.annotation.Keep
import androidx.annotation.UiThread
import com.google.gson.Gson
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.telemetry.UserFeedback
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackItem
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class CarFeedbackSender {

    private val gson = Gson()

    /**
     * Posts the selected item and records it in the history recorder.
     *
     * @return `true` when the feedback reached its destination: an item with a navigation
     * feedback type was posted, or an item without one (recorded in history only, by design) was
     * pushed to the history recorder. `false` when nothing was sent, including when feedback
     * metadata is unavailable because the trip session is stopped.
     */
    @UiThread
    fun send(
        selectedItem: CarFeedbackItem,
        encodedSnapshot: String?,
        sourceScreenSimpleName: String,
    ): Boolean {
        val mapboxNavigation = MapboxNavigationApp.current() ?: return false
        val feedbackMetadata = provideFeedbackMetadata(mapboxNavigation)

        // Metadata is unavailable when the trip session is stopped; the feedback is then not
        // posted and the user is told so, rather than posting it detached from any session.
        val posted = if (selectedItem.navigationFeedbackType != null && feedbackMetadata != null) {
            postUserFeedback(
                mapboxNavigation,
                selectedItem,
                selectedItem.navigationFeedbackType,
                encodedSnapshot,
                feedbackMetadata,
            )
        } else {
            false
        }

        // Search feedback reasons are only recorded in history, not sent to the Search SDK's
        // analytics service: that needs the SearchResult/SearchSuggestion and ResponseInfo
        // that originated a search, and those don't reach the feedback screens.
        // See CarFeedbackPollProvider.getPlaceFeedbackPoll.

        // Collect feedback in the history recorder.
        val recorded = recordFeedbackInHistory(
            mapboxNavigation,
            CarFeedbackHistoryEvent(
                sourceScreen = sourceScreenSimpleName,
                carFeedbackItem = selectedItem,
                screenshotAttached = encodedSnapshot != null,
                feedbackMetadata = feedbackMetadata,
            ),
        )
        return if (selectedItem.navigationFeedbackType != null) posted else recorded
    }

    // Throws when the trip session is stopped.
    private fun provideFeedbackMetadata(mapboxNavigation: MapboxNavigation): FeedbackMetadata? {
        return runCatching { mapboxNavigation.provideFeedbackMetadataWrapper().get() }
            .onFailure { logAndroidAutoFailure("Car feedback metadata is unavailable", it) }
            .getOrNull()
    }

    private fun postUserFeedback(
        mapboxNavigation: MapboxNavigation,
        selectedItem: CarFeedbackItem,
        feedbackType: String,
        encodedSnapshot: String?,
        feedbackMetadata: FeedbackMetadata,
    ): Boolean {
        return runCatching {
            val userFeedback = UserFeedback.Builder(
                feedbackType = feedbackType,
                description = "Android Auto selection: ${selectedItem.carFeedbackTitle}",
            )
                .feedbackSubTypes(selectedItem.navigationFeedbackSubType.orEmpty())
                .screenshot(encodedSnapshot)
                .feedbackMetadata(feedbackMetadata)
                .build()
            mapboxNavigation.postUserFeedback(userFeedback)
        }.onFailure {
            logAndroidAutoFailure("Car feedback could not be posted", it)
        }.isSuccess
    }

    private fun recordFeedbackInHistory(
        mapboxNavigation: MapboxNavigation,
        historyEvent: CarFeedbackHistoryEvent,
    ): Boolean {
        return runCatching {
            val eventJson = gson.toJson(historyEvent)
            mapboxNavigation.historyRecorder.pushHistory(HISTORY_EVENT_TYPE, eventJson)
        }.onFailure {
            logAndroidAutoFailure("Car feedback could not be recorded in history", it)
        }.isSuccess
    }

    private companion object {
        private const val HISTORY_EVENT_TYPE = "car_feedback_sent"
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Keep
internal data class CarFeedbackHistoryEvent(
    val sourceScreen: String,
    val carFeedbackItem: CarFeedbackItem? = null,
    val screenshotAttached: Boolean = false,
    val feedbackMetadata: FeedbackMetadata? = null,
)
