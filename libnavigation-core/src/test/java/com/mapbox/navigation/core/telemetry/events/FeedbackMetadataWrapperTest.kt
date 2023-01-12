package com.mapbox.navigation.core.telemetry.events

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigator.UserFeedbackHandle
import com.mapbox.navigator.UserFeedbackMetadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertNotNull
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
class FeedbackMetadataWrapperTest {

    @Test
    fun getMethodTest() {
        val mockMetadata = mockk<UserFeedbackMetadata>()
        val mockUserFeedbackHandle = mockk<UserFeedbackHandle> {
            every { metadata } returns mockMetadata
        }
        val feedbackMetadataWrapper = provideFeedbackMetadataWrapper(mockUserFeedbackHandle)

        val feedbackMetadata = feedbackMetadataWrapper.get()

        assertNotNull(feedbackMetadata)
        verify (exactly = 1) { mockUserFeedbackHandle.metadata }
    }

    private fun provideFeedbackMetadataWrapper(
        mockUserFeedbackHandle: UserFeedbackHandle = mockk()
    ): FeedbackMetadataWrapper =
        FeedbackMetadataWrapper(mockUserFeedbackHandle)
}
