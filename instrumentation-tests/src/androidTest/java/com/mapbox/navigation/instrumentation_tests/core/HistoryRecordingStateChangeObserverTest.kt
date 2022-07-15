package com.mapbox.navigation.instrumentation_tests.core

import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventSetRoute
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.MutableListFlow
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.routes.NavigationRoutesProvider
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.reflect.KClass

class HistoryRecordingStateChangeObserverTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()
    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation() = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .build()
            )
        }
    }

    @Test
    fun history_recording_observer_events() = sdkTest {
        val _eventsFlow = MutableListFlow<HistoryRecordingStateChangeEvent>()
        val eventsFlow = _eventsFlow.asListFlow()
        observeHistoryRecordingEvents(_eventsFlow)
        val nonEmptyRoutes = NavigationRoutesProvider.dc_very_short(activity)
        val otherNonEmptyRoutes = NavigationRoutesProvider.dc_very_short_two_legs(activity)

        assertFalse(eventsFlow.hasNextElement())
        // start free drive
        mapboxNavigation.startTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.START, NavigationSessionState.FreeDrive::class),
            eventsFlow.nextElement()
        )
        // stop free drive + start active guidance
        mapboxNavigation.setNavigationRoutes(nonEmptyRoutes)
        assertEquals(
            listOf(
                HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.STOP, NavigationSessionState.FreeDrive::class),
                HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.START, NavigationSessionState.ActiveGuidance::class),
            ),
            eventsFlow.nextElements(2)
        )
        // do nothing
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(otherNonEmptyRoutes)
        assertFalse(eventsFlow.hasNextElement())
        // stop active guidance + start free drive
        mapboxNavigation.setNavigationRoutes(emptyList())
        assertEquals(
            listOf(
                HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.STOP, NavigationSessionState.ActiveGuidance::class),
                HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.START, NavigationSessionState.FreeDrive::class),
            ),
            eventsFlow.nextElements(2)
        )
        // stop free drive
        mapboxNavigation.stopTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.STOP, NavigationSessionState.FreeDrive::class),
            eventsFlow.nextElement()
        )
        // do nothing
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(nonEmptyRoutes)
        assertFalse(eventsFlow.hasNextElement())
        // start active guidance
        mapboxNavigation.startTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.START, NavigationSessionState.ActiveGuidance::class),
            eventsFlow.nextElement()
        )
        // stop active guidance
        mapboxNavigation.stopTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.STOP, NavigationSessionState.ActiveGuidance::class),
            eventsFlow.nextElement()
        )
        // do nothing
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()
        assertFalse(eventsFlow.hasNextElement())
        // start free drive
        mapboxNavigation.startTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.START, NavigationSessionState.FreeDrive::class),
            eventsFlow.nextElement()
        )
        // stop free drive + start active guidance +
        // + cancel active guidance + start free drive
        // because of the invalid route
        mapboxNavigation.setNavigationRoutes(nonEmptyRoutes, 16)
        assertEquals(
            listOf(
                HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.STOP, NavigationSessionState.FreeDrive::class),
                HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.START, NavigationSessionState.ActiveGuidance::class),
                HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.CANCEL, NavigationSessionState.ActiveGuidance::class),
                HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.START, NavigationSessionState.FreeDrive::class),
            ),
            eventsFlow.nextElements(4)
        )
        // stop free drive
        mapboxNavigation.stopTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(HistoryRecordingStateChangeEventType.STOP, NavigationSessionState.FreeDrive::class),
            eventsFlow.nextElement()
        )
        assertFalse(eventsFlow.hasNextElement())
    }

    @Test
    fun history_recording_observer_ensures_first_set_route_event() = sdkTest {
        val routes = NavigationRoutesProvider.dc_very_short(activity)
        mapboxNavigation.startTripSession()

        val historyFilePath = awaitStopActiveGuidanceRecording(routes)
        assertNotNull(historyFilePath)
        val historyEvents = mutableListOf<HistoryEvent>()
        val reader = MapboxHistoryReader(historyFilePath!!)
        while (reader.hasNext()) {
            historyEvents.add(reader.next())
        }
        val setRouteEvent = historyEvents.filterIsInstance<HistoryEventSetRoute>().first()
        assertEquals(routes[0].id, setRouteEvent.navigationRoute!!.id)
    }

    private fun observeHistoryRecordingEvents(eventsFlow: MutableListFlow<HistoryRecordingStateChangeEvent>) {
        val observer = object : HistoryRecordingStateChangeObserver {

            override fun onShouldStartRecording(state: NavigationSessionState) {
                println("[HistoryRecordingStateHandler] should start: $state")
                eventsFlow.value += HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    state::class
                )
            }

            override fun onShouldStopRecording(state: NavigationSessionState) {
                println("[HistoryRecordingStateHandler] should stop: $state")
                eventsFlow.value += HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.STOP,
                    state::class
                )
            }

            override fun onShouldCancelRecording(state: NavigationSessionState) {
                eventsFlow.value += HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.CANCEL,
                    state::class
                )
            }
        }
        mapboxNavigation.registerHistoryRecordingStateChangeObserver(observer)
    }

    private suspend fun awaitStopActiveGuidanceRecording(routes: List<NavigationRoute>) = suspendCancellableCoroutine<String?> { continuation ->
        val observer = object : HistoryRecordingStateChangeObserver {
            override fun onShouldStartRecording(state: NavigationSessionState) {
                mapboxNavigation.historyRecorder.startRecording()
            }

            override fun onShouldStopRecording(state: NavigationSessionState) {
                mapboxNavigation.historyRecorder.stopRecording { filePath ->
                    if (state is NavigationSessionState.ActiveGuidance) {
                        mapboxNavigation.unregisterHistoryRecordingStateChangeObserver(this)
                        continuation.resume(filePath) {}
                    }
                }
            }

            override fun onShouldCancelRecording(state: NavigationSessionState) {
                mapboxNavigation.historyRecorder.stopRecording {
                    if (state is NavigationSessionState.ActiveGuidance) {
                        mapboxNavigation.unregisterHistoryRecordingStateChangeObserver(this)
                        continuation.resume(null) {}
                    }
                }
            }
        }
        continuation.invokeOnCancellation {
            mapboxNavigation.unregisterHistoryRecordingStateChangeObserver(observer)
        }
        mapboxNavigation.registerHistoryRecordingStateChangeObserver(observer)
        mapboxNavigation.setNavigationRoutes(routes)
        mapboxNavigation.setNavigationRoutes(emptyList())
    }
}

class HistoryRecordingStateChangeEvent(
    val type: HistoryRecordingStateChangeEventType,
    val state: KClass<out NavigationSessionState>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryRecordingStateChangeEvent

        if (type != other.type) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }

    override fun toString(): String {
        return "Event(type=$type, state=${state.simpleName})"
    }

}

enum class HistoryRecordingStateChangeEventType { START, STOP, CANCEL }

