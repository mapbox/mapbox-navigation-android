package com.mapbox.navigation.core.replay.history

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.TripSessionResetCallback
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.MapboxHistoryReaderProvider
import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventSetRoute
import com.mapbox.navigation.core.history.model.HistoryEventUpdateLocation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class ReplayHistorySessionTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val replayer: MapboxReplayer = mockk(relaxed = true)
    private val historyReader: MapboxHistoryReader = mockk(relaxed = true)

    private val sut = ReplayHistorySession()

    @Before
    fun setup() {
        mockkObject(MapboxHistoryReaderProvider)
        every { MapboxHistoryReaderProvider.create(any()) } returns historyReader
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttached will call startReplayTripSession`() {
        val mapboxNavigation = mockMapboxNavigation()

        sut.onAttached(mapboxNavigation)

        verify(exactly = 1) { mapboxNavigation.startReplayTripSession() }
    }

    @Test
    fun `onAttached will call MapboxReplayer#play`() {
        val mapboxNavigation = mockMapboxNavigation()

        sut.onAttached(mapboxNavigation)

        verify(exactly = 1) { replayer.play() }
    }

    @Test
    fun `setHistoryFile after onAttached will clear events and reset the trip`() {
        val mapboxNavigation = mockMapboxNavigation()

        sut.onAttached(mapboxNavigation)
        sut.setHistoryFile("test_file_path")

        verifyOrder {
            mapboxNavigation.startReplayTripSession()
            replayer.clearEvents()
            mapboxNavigation.setNavigationRoutes(emptyList())
            mapboxNavigation.resetTripSession(any())
            replayer.play()
            // setHistoryFile was called
            replayer.clearEvents()
            mapboxNavigation.setNavigationRoutes(emptyList())
            MapboxHistoryReaderProvider.create("test_file_path")
            mapboxNavigation.resetTripSession(any())
            replayer.play()
        }
        verify(exactly = 1) {
            mapboxNavigation.startReplayTripSession()
            MapboxHistoryReaderProvider.create(any())
        }
    }

    @Test
    fun `setHistoryFile before onAttached will initialize once`() {
        val mapboxNavigation = mockMapboxNavigation()

        sut.setHistoryFile("test_file_path")
        sut.onAttached(mapboxNavigation)

        verifyOrder {
            mapboxNavigation.startReplayTripSession()
            replayer.clearEvents()
            mapboxNavigation.setNavigationRoutes(emptyList())
            MapboxHistoryReaderProvider.create("test_file_path")
            mapboxNavigation.resetTripSession(any())
            replayer.play()
        }
        verify(exactly = 1) {
            mapboxNavigation.startReplayTripSession()
            replayer.clearEvents()
            mapboxNavigation.setNavigationRoutes(any())
            MapboxHistoryReaderProvider.create(any())
            mapboxNavigation.resetTripSession(any())
            replayer.play()
        }
    }

    @Test
    fun `onDetached will clean up but will not stopTripSession`() {
        val mapboxNavigation = mockMapboxNavigation()

        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        verifyOrder {
            mapboxNavigation.startReplayTripSession()
            replayer.stop()
            replayer.registerObserver(any())
            replayer.clearEvents()
            mapboxNavigation.setNavigationRoutes(emptyList())
            mapboxNavigation.resetTripSession(any())
            replayer.play()
            // onDetached called
            replayer.unregisterObserver(any())
            replayer.stop()
            replayer.clearEvents()
        }
        verify(exactly = 1) {
            replayer.play()
        }
    }

    @Test
    fun `should push events from history file`() {
        val mapboxNavigation = mockMapboxNavigation()
        val eventCount = 100
        every { historyReader.hasNext() } returnsMany (0..eventCount)
            .map { it != eventCount }
        every { historyReader.next() } returnsMany (1..eventCount)
            .map { value ->
                mockk<HistoryEventUpdateLocation> {
                    every { eventTimestamp } returns value.toDouble()
                    every { location } returns mockk()
                }
            }
        val eventObserver = slot<ReplayEventsObserver>()
        every { replayer.registerObserver(capture(eventObserver)) } just runs
        val eventSlot = mutableListOf<List<ReplayEventBase>>()
        every { replayer.pushEvents(capture(eventSlot)) } answers {
            eventObserver.captured.replayEvents(firstArg())
            replayer
        }

        sut.setOptions(mockOptions())
        sut.onAttached(mapboxNavigation)

        val capturedEvents = eventSlot.flatten()
        assertEquals(100, capturedEvents.size)
    }

    @Test
    fun `should setNavigationRoutes from history file when option is enabled`() {
        val mapboxNavigation = mockMapboxNavigation()
        val options = mockOptions()
        every { options.enableSetRoute } returns true
        every { historyReader.hasNext() } returnsMany listOf(true, false)
        every { historyReader.next() } returnsMany listOf(
            mockk<HistoryEventSetRoute> {
                every { eventTimestamp } returns 11.0
                every { navigationRoute } returns mockk()
            },
        )
        val eventObserver = slot<ReplayEventsObserver>()
        every { replayer.registerObserver(capture(eventObserver)) } just runs
        val eventSlot = mutableListOf<List<ReplayEventBase>>()
        every { replayer.pushEvents(capture(eventSlot)) } answers {
            eventObserver.captured.replayEvents(firstArg())
            replayer
        }

        sut.setOptions(options)
        sut.onAttached(mapboxNavigation)

        verify { mapboxNavigation.setNavigationRoutes(any()) }
    }

    @Test
    fun `should not setNavigationRoutes from history file when option is disabled`() {
        val mapboxNavigation = mockMapboxNavigation()
        val options = mockOptions()
        every { options.enableSetRoute } returns false
        every { historyReader.hasNext() } returnsMany listOf(true, false)
        every { historyReader.next() } returnsMany listOf(
            mockk<HistoryEventSetRoute> {
                every { eventTimestamp } returns 11.0
                every { navigationRoute } returns mockk()
            },
        )
        val eventObserver = slot<ReplayEventsObserver>()
        every { replayer.registerObserver(capture(eventObserver)) } just runs
        val eventSlot = mutableListOf<List<ReplayEventBase>>()
        every { replayer.pushEvents(capture(eventSlot)) } answers {
            eventObserver.captured.replayEvents(firstArg())
            replayer
        }

        sut.setOptions(options)
        sut.onAttached(mapboxNavigation)

        verify { mapboxNavigation.setNavigationRoutes(any()) }
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val context: Context = mockk(relaxed = true)
        val options: NavigationOptions = mockk {
            every { applicationContext } returns context
        }
        val routesObserver = slot<RoutesObserver>()
        val routeProgressObserver = slot<RouteProgressObserver>()
        return mockk(relaxed = true) {
            every { mapboxReplayer } returns replayer
            every { navigationOptions } returns options
            every { registerRoutesObserver(capture(routesObserver)) } just runs
            every { registerRouteProgressObserver(capture(routeProgressObserver)) } just runs
            every { resetTripSession(any()) } answers {
                firstArg<TripSessionResetCallback>().onTripSessionReset()
            }
        }
    }

    private fun mockOptions(): ReplayHistorySessionOptions = mockk {
        every { filePath } returns "test_file_path"
        every { replayHistoryMapper } returns mockk {
            every { mapToReplayEvent(any()) } answers {
                mockk {
                    every { eventTimestamp } returns firstArg<HistoryEvent>().eventTimestamp
                }
            }
        }
        every { enableSetRoute } returns true
    }
}
