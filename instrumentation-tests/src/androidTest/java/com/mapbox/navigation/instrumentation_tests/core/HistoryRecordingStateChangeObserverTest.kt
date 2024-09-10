package com.mapbox.navigation.instrumentation_tests.core

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventSetRoute
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.unregisterHistoryRecordingStateChangeObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndAwaitError
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForAlternativesUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.http.MockRoutingTileEndpointErrorRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.MockRoute
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.reflect.KClass

class HistoryRecordingStateChangeObserverTest :
    BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)
    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation() = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @Test
    fun history_recording_observer_events() = sdkTest {
        createMapboxNavigation()
        val eventsChannel = Channel<HistoryRecordingStateChangeEvent>(Channel.UNLIMITED)
        observeHistoryRecordingEvents(eventsChannel)
        val nonEmptyRoutes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        val otherNonEmptyRoutes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short_two_legs(activity),
        )

        checkHasNoNextElement(eventsChannel)
        mapboxNavigation.startTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.START,
                HistoryRecordingSessionState.FreeDrive::class,
            ),
            eventsChannel.receive(),
        )
        mapboxNavigation.setNavigationRoutes(nonEmptyRoutes)
        assertEquals(
            listOf(
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.STOP,
                    HistoryRecordingSessionState.FreeDrive::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    HistoryRecordingSessionState.ActiveGuidance::class,
                ),
            ),
            eventsChannel.receive(2),
        )
        // set other non-empty routes - no state transitions - do nothing
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(otherNonEmptyRoutes)
        checkHasNoNextElement(eventsChannel)
        // set invalid routes, but has other non-empty routes - do nothing
        mapboxNavigation.setNavigationRoutesAndAwaitError(nonEmptyRoutes, legIndex = 15)
        checkHasNoNextElement(eventsChannel)
        // alternatives - do nothing
        mapboxNavigation.setNavigationRoutesAndWaitForAlternativesUpdate(
            otherNonEmptyRoutes + nonEmptyRoutes,
        )
        checkHasNoNextElement(eventsChannel)
        mapboxNavigation.setNavigationRoutes(emptyList())
        assertEquals(
            listOf(
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.STOP,
                    HistoryRecordingSessionState.ActiveGuidance::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    HistoryRecordingSessionState.FreeDrive::class,
                ),
            ),
            eventsChannel.receive(2),
        )
        mapboxNavigation.stopTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.STOP,
                HistoryRecordingSessionState.FreeDrive::class,
            ),
            eventsChannel.receive(),
        )
        // trip session is stopped - still Idle - do nothing
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(nonEmptyRoutes)
        checkHasNoNextElement(eventsChannel)
        mapboxNavigation.startTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.START,
                HistoryRecordingSessionState.ActiveGuidance::class,
            ),
            eventsChannel.receive(),
        )
        mapboxNavigation.stopTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.STOP,
                HistoryRecordingSessionState.ActiveGuidance::class,
            ),
            eventsChannel.receive(),
        )
        // trip session stopped - still Idle - do nothing
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()
        checkHasNoNextElement(eventsChannel)
        mapboxNavigation.startTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.START,
                HistoryRecordingSessionState.FreeDrive::class,
            ),
            eventsChannel.receive(),
        )
        // immediately cancel active guidance because of the invalid route
        mapboxNavigation.setNavigationRoutes(otherNonEmptyRoutes, initialLegIndex = 16)
        assertEquals(
            listOf(
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.STOP,
                    HistoryRecordingSessionState.FreeDrive::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    HistoryRecordingSessionState.ActiveGuidance::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.CANCEL,
                    HistoryRecordingSessionState.ActiveGuidance::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    HistoryRecordingSessionState.FreeDrive::class,
                ),
            ),
            eventsChannel.receive(4),
        )
        mapboxNavigation.stopTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.STOP,
                HistoryRecordingSessionState.FreeDrive::class,
            ),
            eventsChannel.receive(),
        )
        checkHasNoNextElement(eventsChannel)
    }

    @Test
    fun history_recording_observer_receives_current_state_event_for_active_sessions() = sdkTest {
        createMapboxNavigation()
        val nonEmptyRoutes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        val eventsChannelIdle = Channel<HistoryRecordingStateChangeEvent>(Channel.UNLIMITED)
        val eventsChannelFreeDrive = Channel<HistoryRecordingStateChangeEvent>(Channel.UNLIMITED)
        val eventsChannelActiveGuidance =
            Channel<HistoryRecordingStateChangeEvent>(Channel.UNLIMITED)

        observeHistoryRecordingEvents(eventsChannelIdle)
        checkHasNoNextElement(eventsChannelIdle)

        mapboxNavigation.startTripSession()
        observeHistoryRecordingEvents(eventsChannelFreeDrive)

        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.START,
                HistoryRecordingSessionState.FreeDrive::class,
            ),
            eventsChannelFreeDrive.receive(),
        )

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(nonEmptyRoutes)
        observeHistoryRecordingEvents(eventsChannelActiveGuidance)

        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.START,
                HistoryRecordingSessionState.ActiveGuidance::class,
            ),
            eventsChannelActiveGuidance.receive(),
        )
    }

    @Test
    fun history_recording_observer_route_refresh() = sdkTest {
        val mockRoute = RoutesProvider.dc_very_short(activity)
        setUpMockRequestHandlersForRefresh(mockRoute)
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .routeRefreshOptions(generateRouteRefreshOptions())
                .navigatorPredictionMillis(0L)
                .build(),
        )
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            mockRoute,
        )

        val eventsChannel = Channel<HistoryRecordingStateChangeEvent>(Channel.UNLIMITED)
        observeHistoryRecordingEvents(eventsChannel)

        mapboxNavigation.startTripSession()
        stayOnPosition(mockRoute.routeWaypoints.first())
        mapboxNavigation.setNavigationRoutes(routes)
        assertEquals(
            listOf(
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    HistoryRecordingSessionState.FreeDrive::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.STOP,
                    HistoryRecordingSessionState.FreeDrive::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    HistoryRecordingSessionState.ActiveGuidance::class,
                ),
            ),
            eventsChannel.receive(3),
        )
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()
        checkHasNoNextElement(eventsChannel)

        mapboxNavigation.stopTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.STOP,
                HistoryRecordingSessionState.ActiveGuidance::class,
            ),
            eventsChannel.receive(),
        )
    }

    @Test
    fun history_recording_observer_reroute() = sdkTest {
        createMapboxNavigation()
        val mockRoute = RoutesProvider.dc_very_short(activity)
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            mockRoute,
        )
        val offRouteLocationUpdate = getOffRouteLocation(mockRoute.routeWaypoints.first())
        setUpMockRequestHandlersForReroute(mockRoute, offRouteLocationUpdate)

        val eventsChannel = Channel<HistoryRecordingStateChangeEvent>(Channel.UNLIMITED)
        observeHistoryRecordingEvents(eventsChannel)

        mapboxNavigation.startTripSession()
        stayOnPosition(offRouteLocationUpdate)
        mapboxNavigation.setNavigationRoutes(routes)
        assertEquals(
            listOf(
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    HistoryRecordingSessionState.FreeDrive::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.STOP,
                    HistoryRecordingSessionState.FreeDrive::class,
                ),
                HistoryRecordingStateChangeEvent(
                    HistoryRecordingStateChangeEventType.START,
                    HistoryRecordingSessionState.ActiveGuidance::class,
                ),
            ),
            eventsChannel.receive(3),
        )
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
            .first()
        checkHasNoNextElement(eventsChannel)

        mapboxNavigation.stopTripSession()
        assertEquals(
            HistoryRecordingStateChangeEvent(
                HistoryRecordingStateChangeEventType.STOP,
                HistoryRecordingSessionState.ActiveGuidance::class,
            ),
            eventsChannel.receive(),
        )
    }

    @Test
    fun history_recording_observer_ensures_first_set_route_event() = sdkTest {
        createMapboxNavigation()
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        mapboxNavigation.startTripSession()

        val historyFilePath = awaitStopActiveGuidanceRecording(routes)
        assertNotNull(historyFilePath)
        val historyEvents = mutableListOf<HistoryEvent>()
        val reader = MapboxHistoryReader(historyFilePath!!)
        while (reader.hasNext()) {
            historyEvents.add(reader.next())
        }
        val setRouteEvents = historyEvents.filterIsInstance<HistoryEventSetRoute>()
        assertEquals(routes[0].id, setRouteEvents.firstNotNullOf { it.navigationRoute }.id)
    }

    private fun generateRouteRefreshOptions(): RouteRefreshOptions {
        val routeRefreshOptions = RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
            .build()
        RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
            isAccessible = true
            set(routeRefreshOptions, 3_000L)
        }
        return routeRefreshOptions
    }

    private fun setUpMockRequestHandlersForRefresh(mockRoute: MockRoute) {
        mockWebServerRule.requestHandlers.clear()
        val failByRequestRouteRefreshResponse = FailByRequestMockRequestHandler(
            MockDirectionsRefreshHandler(
                mockRoute.routeResponse.uuid()!!,
                readRawFileText(activity, R.raw.route_response_dc_very_short_refreshed),
            ),
        )
        mockWebServerRule.requestHandlers.add(failByRequestRouteRefreshResponse)
        mockWebServerRule.requestHandlers.add(
            MockRoutingTileEndpointErrorRequestHandler(),
        )
    }

    private fun setUpMockRequestHandlersForReroute(
        mockRoute: MockRoute,
        offRouteLocationUpdate: Point,
    ) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(activity, R.raw.reroute_response_dc_very_short),
                expectedCoordinates = listOf(
                    Point.fromLngLat(
                        offRouteLocationUpdate.longitude(),
                        offRouteLocationUpdate.latitude(),
                    ),
                    mockRoute.routeWaypoints.last(),
                ),
                relaxedExpectedCoordinates = true,
            ),
        )
    }

    private fun getOffRouteLocation(originLocation: Point): Point =
        Point.fromLngLat(originLocation.longitude(), originLocation.latitude() + 0.002)

    private fun createMapboxNavigation() {
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .build(),
        )
    }

    private fun stayOnPosition(position: Point) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = position.latitude()
                longitude = position.longitude()
            },
            times = 120,
        )
    }

    private fun observeHistoryRecordingEvents(
        eventsChannel: Channel<HistoryRecordingStateChangeEvent>,
    ) {
        val observer = object : HistoryRecordingStateChangeObserver {

            override fun onShouldStartRecording(state: HistoryRecordingSessionState) {
                eventsChannel.trySend(
                    HistoryRecordingStateChangeEvent(
                        HistoryRecordingStateChangeEventType.START,
                        state::class,
                    ),
                )
            }

            override fun onShouldStopRecording(state: HistoryRecordingSessionState) {
                eventsChannel.trySend(
                    HistoryRecordingStateChangeEvent(
                        HistoryRecordingStateChangeEventType.STOP,
                        state::class,
                    ),
                )
            }

            override fun onShouldCancelRecording(state: HistoryRecordingSessionState) {
                eventsChannel.trySend(
                    HistoryRecordingStateChangeEvent(
                        HistoryRecordingStateChangeEventType.CANCEL,
                        state::class,
                    ),
                )
            }
        }
        mapboxNavigation.registerHistoryRecordingStateChangeObserver(observer)
    }

    private suspend fun awaitStopActiveGuidanceRecording(
        routes: List<NavigationRoute>,
    ) = suspendCancellableCoroutine<String?> { continuation ->
        val observer = object : HistoryRecordingStateChangeObserver {
            override fun onShouldStartRecording(state: HistoryRecordingSessionState) {
                mapboxNavigation.historyRecorder.startRecording()
            }

            override fun onShouldStopRecording(state: HistoryRecordingSessionState) {
                mapboxNavigation.historyRecorder.stopRecording { filePath ->
                    if (state is HistoryRecordingSessionState.ActiveGuidance) {
                        mapboxNavigation.unregisterHistoryRecordingStateChangeObserver(this)
                        continuation.resume(filePath)
                    }
                }
            }

            override fun onShouldCancelRecording(state: HistoryRecordingSessionState) {
                mapboxNavigation.historyRecorder.stopRecording {
                    if (state is HistoryRecordingSessionState.ActiveGuidance) {
                        mapboxNavigation.unregisterHistoryRecordingStateChangeObserver(this)
                        continuation.resume(null)
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

data class HistoryRecordingStateChangeEvent(
    val type: HistoryRecordingStateChangeEventType,
    val state: KClass<out HistoryRecordingSessionState>,
) {

    override fun toString(): String {
        return "Event(type=$type, state=${state.simpleName})"
    }
}

enum class HistoryRecordingStateChangeEventType { START, STOP, CANCEL }

private suspend fun <T> ReceiveChannel<T>.receive(number: Int): List<T> {
    val result = mutableListOf<T>()
    repeat(number) {
        result.add(receive())
    }
    return result
}

private fun <T> checkHasNoNextElement(channel: Channel<T>) {
    assertTrue(channel.tryReceive().isFailure)
}
