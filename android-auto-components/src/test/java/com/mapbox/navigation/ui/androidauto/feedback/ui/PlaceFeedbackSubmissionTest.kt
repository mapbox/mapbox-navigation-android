package com.mapbox.navigation.ui.androidauto.feedback.ui

import android.content.Context
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.telemetry.UserFeedback
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackPollProvider
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackSender
import com.mapbox.navigation.ui.androidauto.internal.getCarIcon
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.ui.androidauto.testing.TestOnDoneCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Selecting a default place feedback option goes through the real screen and sender and ends up
 * as a navigation feedback event.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PlaceFeedbackSubmissionTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val carContext: CarContext = mockk {
        every { getString(any()) } answers { context.getString(firstArg()) }
        every { getCarService(AppManager::class.java) } returns mockk(relaxed = true)
    }
    private val mapboxCarContext: MapboxCarContext = mockk {
        every { carContext } returns this@PlaceFeedbackSubmissionTest.carContext
    }
    private val feedbackMetadata: FeedbackMetadata = mockk()
    private val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { provideFeedbackMetadataWrapper() } returns mockk {
            every { get() } returns feedbackMetadata
        }
    }

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.ui.androidauto.internal.IconUtilsKt")
        every { carContext.getCarIcon(any()) } returns CarFeedbackIcon.Local(CarIcon.APP_ICON)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
    }

    @Test
    fun `selecting incorrect location posts other issue navigation feedback`() {
        val poll = CarFeedbackPollProvider().getPlaceFeedbackPoll(carContext)
        val title = context.getString(R.string.car_feedback_search_incorrect_location)
        val screen = object : CarGridFeedbackScreen(
            mapboxCarContext,
            SOURCE_SCREEN,
            CarFeedbackSender(),
            poll,
            { SNAPSHOT },
        ) {
            override fun onFinish() = Unit
        }

        clickItem(screen, poll.options.indexOfFirst { it.title == title })

        val userFeedback = slot<UserFeedback>()
        verify(exactly = 1) { mapboxNavigation.postUserFeedback(capture(userFeedback)) }
        with(userFeedback.captured) {
            assertEquals(FeedbackEvent.OTHER_ISSUE, feedbackType)
            assertEquals("Android Auto selection: $title", description)
            assertEquals(SNAPSHOT, screenshot)
            assertEquals(feedbackMetadata, this.feedbackMetadata)
        }
    }

    private fun clickItem(screen: CarGridFeedbackScreen, index: Int) {
        val template = screen.onGetTemplate() as GridTemplate
        val gridItem = template.singleList!!.items[index] as GridItem
        val callback = TestOnDoneCallback()
        gridItem.onClickDelegate!!.sendClick(callback)
        callback.assertSuccess()
    }

    private companion object {
        private const val SOURCE_SCREEN = "SEARCH"
        private const val SNAPSHOT = "snapshot"
    }
}
