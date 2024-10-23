package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.telemetry.UserFeedback.Companion.mapToNative
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigator.ScreenshotFormat
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class UserFeedbackTest {

    @Test
    fun `test mapToNative()`() {
        val userFeedback = UserFeedback.Builder(
            feedbackType = FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
            description = "test-description",
        )
            .feedbackSubTypes(listOf(FeedbackEvent.MANEUVER_INCORRECT))
            .screenshot("test-base-64")
            .build()

        val nativeUserFeedback = com.mapbox.navigator.UserFeedback(
            FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
            listOf(FeedbackEvent.MANEUVER_INCORRECT),
            "test-description",
            ScreenshotFormat(null, "test-base-64"),
        )

        assertEquals(nativeUserFeedback, userFeedback.mapToNative())
    }
}
