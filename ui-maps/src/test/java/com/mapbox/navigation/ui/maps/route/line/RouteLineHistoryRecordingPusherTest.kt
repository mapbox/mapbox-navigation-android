package com.mapbox.navigation.ui.maps.route.line

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.history.HistoryRecorderChooser
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineEvent
import com.mapbox.navigation.ui.maps.util.MutexBasedScope
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteLineHistoryRecordingPusherTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val observerSlot = mutableListOf<(MapboxHistoryRecorder?) -> Unit>()
    private val historyRecorderChooser = mockk<HistoryRecorderChooser>(relaxed = true)
    private val historyRecorderChooserFactory =
        mockk<HistoryRecorderChooserFactory>(relaxed = true) {
            every { create(any(), any()) } returns historyRecorderChooser
        }

    private lateinit var pusher: RouteLineHistoryRecordingPusher

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationProvider)
        pusher = RouteLineHistoryRecordingPusher(
            coroutineRule.testDispatcher,
            MutexBasedScope(coroutineRule.coroutineScope),
            historyRecorderChooserFactory,
        )

        mockkObject(RouteLineHistoryRecordingPusherProvider)
        every { RouteLineHistoryRecordingPusherProvider.instance } returns pusher
    }

    @After
    fun tearDown() {
        unmockkObject(RouteLineHistoryRecordingPusherProvider)
        unmockkObject(MapboxNavigationProvider)
    }

    @Test
    fun pushEventOrAddToQueueNoRecorder() {
        val json = "some json"
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } returns json
        }
        pusher.pushEventOrAddToQueue { event }

        verify(exactly = 0) { event.toJson() }

        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)

        verify { recorder.pushHistory("mbx.RouteLine", json) }
    }

    @Test
    fun pushEventOrAddToQueueHasRecorder() {
        val json = "some json"
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } returns json
        }
        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)

        pusher.pushEventOrAddToQueue { event }

        verify { recorder.pushHistory("mbx.RouteLine", json) }
        clearMocks(recorder, event, answers = false)

        val recorder2 = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder2)

        verify(exactly = 0) { event.toJson() }
        verify(exactly = 0) { recorder.pushHistory(any(), any()) }
        verify(exactly = 0) { recorder2.pushHistory(any(), any()) }
    }

    @Test
    fun pushEventOrAddToQueueAfterRecorderDisappeared() {
        val json = "some json"
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } returns json
        }
        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)
        onRecorderDisabled()

        pusher.pushEventOrAddToQueue { event }

        verify(exactly = 0) { event.toJson() }

        val recorder2 = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder2)

        verify { recorder2.pushHistory("mbx.RouteLine", json) }
    }

    @Test
    fun pushEventOrAddToQueueAfterOnDetached() {
        val json = "some json"
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } returns json
        }
        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)
        pusher.onDetached(mockk(relaxed = true))

        pusher.pushEventOrAddToQueue { event }

        verify(exactly = 0) { event.toJson() }

        val recorder2 = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder2)

        verify { recorder2.pushHistory("mbx.RouteLine", json) }
    }

    @Test
    fun pushEventOrAddToQueueToJsonThrows() {
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } throws NullPointerException()
        }
        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)

        pusher.pushEventOrAddToQueue { event }

        verify(exactly = 0) { recorder.pushHistory(any(), any()) }
    }

    @Test
    fun onDetachedWithOnAttached() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        pusher.onAttached(mapboxNavigation)
        verify { historyRecorderChooserFactory.create(mapboxNavigation, capture(observerSlot)) }

        pusher.onDetached(mapboxNavigation)

        historyRecorderChooser.destroy()
    }

    @Test
    fun pushEventIfEnabledEnabled() {
        val json = "some json"
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } returns json
        }
        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)

        pusher.pushEventIfEnabled { event }

        verify { recorder.pushHistory("mbx.RouteLine", json) }
        clearMocks(recorder, event, answers = false)

        val recorder2 = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder2)

        verify(exactly = 0) { event.toJson() }
        verify(exactly = 0) { recorder.pushHistory(any(), any()) }
        verify(exactly = 0) { recorder2.pushHistory(any(), any()) }
    }

    @Test
    fun pushEventIfEnabledRecorderDisappearedWhileEventWasSerialized() {
        val json = "some json"
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } returns json
        }
        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)

        lateinit var cont: Continuation<RouteLineEvent>
        pusher.pushEventIfEnabled { suspendCoroutine { cont = it } }

        onRecorderDisabled()

        cont.resume(event)

        verify(exactly = 0) { recorder.pushHistory("mbx.RouteLine", any()) }

        onRecorderEnabled(recorder)

        pusher.pushEventIfEnabled { event }

        verify(exactly = 1) { recorder.pushHistory("mbx.RouteLine", json) }
    }

    @Test
    fun pushEventIfEnableDisabled() {
        val json = "some json"
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } returns json
        }
        pusher.pushEventIfEnabled { event }

        verify(exactly = 0) { event.toJson() }

        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)

        verify(exactly = 0) { recorder.pushHistory(any(), any()) }
    }

    @Test
    fun pushEventIfEnabledThrows() {
        val event = mockk<RouteLineEvent>(relaxed = true) {
            every { toJson() } throws NullPointerException()
        }
        val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        onRecorderEnabled(recorder)

        pusher.pushEventIfEnabled { event }

        verify(exactly = 0) { recorder.pushHistory(any(), any()) }
    }

    @Test
    fun onDetachedWithoutOnAttached() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        pusher.onDetached(mapboxNavigation)

        verify(exactly = 0) { historyRecorderChooser.destroy() }
    }

    private fun onRecorderEnabled(recorder: MapboxHistoryRecorder) {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { historyRecorder } returns recorder
            every { navigationOptions } returns mockk {
                every { copilotOptions } returns mockk {
                    every { shouldRecordRouteLineEvents } returns false
                }
                every { historyRecorderOptions } returns mockk {
                    every { shouldRecordRouteLineEvents } returns true
                }
            }
        }
        pusher.onAttached(mapboxNavigation)
        verify { historyRecorderChooserFactory.create(mapboxNavigation, capture(observerSlot)) }
        observerSlot.last()(recorder)
    }

    private fun onRecorderDisabled() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        pusher.onAttached(mapboxNavigation)
        verify { historyRecorderChooserFactory.create(mapboxNavigation, capture(observerSlot)) }
        observerSlot.last()(null)
    }
}
