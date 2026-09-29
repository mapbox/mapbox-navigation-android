package com.mapbox.navigation.ui.androidauto.feedback.ui

import android.content.Context
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackSender
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.ui.androidauto.testing.TestOnDoneCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarGridFeedbackScreenTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appManager: AppManager = mockk(relaxed = true)
    private val carContext: CarContext = mockk {
        every { getString(any()) } answers { context.getString(firstArg()) }
        every { getCarService(AppManager::class.java) } returns appManager
    }
    private val mapboxCarContext: MapboxCarContext = mockk {
        every { carContext } returns this@CarGridFeedbackScreenTest.carContext
    }
    private val sender: CarFeedbackSender = mockk(relaxed = true)
    private var finishCount = 0

    @Test
    fun `template shows the poll title and one grid item per option`() {
        val screen = createScreen(poll("Title", option("First"), option("Second")))

        val template = screen.onGetTemplate() as GridTemplate

        assertEquals("Title", template.title.toString())
        assertEquals(
            listOf("First", "Second"),
            template.singleList!!.items.map { (it as GridItem).title.toString() },
        )
    }

    @Test
    fun `poll without options shows an empty grid`() {
        val screen = createScreen(poll("Title"))

        val template = screen.onGetTemplate() as GridTemplate

        assertTrue(template.singleList!!.items.isEmpty())
    }

    @Test
    fun `selecting an option sends its values, shows a toast and finishes`() {
        val screen = createScreen(
            poll(
                "Title",
                option(
                    "Routing error",
                    type = FeedbackEvent.ROUTING_ERROR,
                    subType = listOf("sub-type"),
                    searchFeedbackReason = "search-reason",
                    favoritesFeedbackReason = "reason",
                ),
            ),
        )
        val item = slot<CarFeedbackItem>()

        clickItem(screen, 0)

        verify(exactly = 1) { sender.send(capture(item), SNAPSHOT, SOURCE_SCREEN) }
        assertEquals("Routing error", item.captured.carFeedbackTitle)
        assertEquals(FeedbackEvent.ROUTING_ERROR, item.captured.navigationFeedbackType)
        assertEquals(listOf("sub-type"), item.captured.navigationFeedbackSubType)
        assertEquals("search-reason", item.captured.searchFeedbackReason)
        assertEquals("reason", item.captured.favoritesFeedbackReason)
        verify(exactly = 1) { appManager.showToast(any(), any()) }
        assertEquals(1, finishCount)
    }

    @Test
    fun `selecting an option without a type still sends it`() {
        val screen = createScreen(poll("Title", option("Other")))
        val item = slot<CarFeedbackItem>()

        clickItem(screen, 0)

        verify(exactly = 1) { sender.send(capture(item), SNAPSHOT, SOURCE_SCREEN) }
        assertNull(item.captured.navigationFeedbackType)
        assertEquals(1, finishCount)
    }

    @Test
    fun `selecting an option with a next poll shows the next poll instead of sending`() {
        val nextPoll = poll("Next title", option("Nested"))
        val screen = createScreen(poll("Title", option("Parent", nextPoll = nextPoll)))

        clickItem(screen, 0)

        verify(exactly = 0) { sender.send(any(), any(), any()) }
        assertEquals(0, finishCount)
        val template = screen.onGetTemplate() as GridTemplate
        assertEquals("Next title", template.title.toString())
        assertEquals("Nested", (template.singleList!!.items[0] as GridItem).title.toString())
    }

    @Test
    fun `selecting an option in the next poll sends that option`() {
        val nextPoll = poll("Next title", option("Nested", type = FeedbackEvent.OTHER_ISSUE))
        val screen = createScreen(poll("Title", option("Parent", nextPoll = nextPoll)))
        val item = slot<CarFeedbackItem>()

        clickItem(screen, 0)
        clickItem(screen, 0)

        verify(exactly = 1) { sender.send(capture(item), SNAPSHOT, SOURCE_SCREEN) }
        assertEquals("Nested", item.captured.carFeedbackTitle)
        assertEquals(1, finishCount)
    }

    private fun clickItem(screen: CarGridFeedbackScreen, index: Int) {
        val template = screen.onGetTemplate() as GridTemplate
        val gridItem = template.singleList!!.items[index] as GridItem
        val callback = TestOnDoneCallback()
        gridItem.onClickDelegate!!.sendClick(callback)
        callback.assertSuccess()
    }

    private fun createScreen(poll: CarFeedbackPoll): CarGridFeedbackScreen {
        return object : CarGridFeedbackScreen(
            mapboxCarContext,
            SOURCE_SCREEN,
            sender,
            poll,
            SNAPSHOT,
        ) {
            override fun onFinish() {
                finishCount++
            }
        }
    }

    private fun poll(title: String, vararg options: CarFeedbackOption) =
        CarFeedbackPoll(title, options.toList())

    private fun option(
        title: String,
        type: String? = null,
        subType: List<String>? = null,
        searchFeedbackReason: String? = null,
        favoritesFeedbackReason: String? = null,
        nextPoll: CarFeedbackPoll? = null,
    ) = CarFeedbackOption(
        title = title,
        icon = CarFeedbackIcon.Local(CarIcon.APP_ICON),
        type = type,
        subType = subType,
        searchFeedbackReason = searchFeedbackReason,
        favoritesFeedbackReason = favoritesFeedbackReason,
        nextPoll = nextPoll,
    )

    private companion object {
        private const val SOURCE_SCREEN = "ROUTE_PREVIEW"
        private const val SNAPSHOT = "snapshot"
    }
}
