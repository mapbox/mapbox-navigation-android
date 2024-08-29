package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventGetStatus
import com.mapbox.navigation.core.history.model.HistoryEventPushHistoryRecord
import com.mapbox.navigation.core.history.model.HistoryEventSetRoute
import com.mapbox.navigation.core.history.model.HistoryEventUpdateLocation
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.routes.MockRoute
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.InputStream

class MapboxHistoryTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeCompleteIdlingResource: RouteProgressStateIdlingResource
    private lateinit var testDirectory: File

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @Before
    fun createTestDirectory() {
        testDirectory = File(context.filesDir, "mapbox_history_test_directory")
            .also { it.mkdirs() }
    }

    @After
    fun deleteTestDirectory() {
        testDirectory.deleteRecursively()
    }

    @Before
    fun setup() {
        Espresso.onIdle()

        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .historyRecorderOptions(
                        HistoryRecorderOptions.Builder()
                            .build(),
                    )
                    .build(),
            )
        }
        routeCompleteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.COMPLETE,
        )
    }

    @Test
    fun verify_history_files_are_recorded_and_readable() {
        // prepare
        val mockRoute = RoutesProvider.dc_very_short(context)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        routeCompleteIdlingResource.register()

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(context)
            .baseUrl(mockWebServerRule.baseUrl)
            .coordinatesList(mockRoute.routeWaypoints).build()

        // execute
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mapboxNavigation.historyRecorder.pushHistory(CUSTOM_EVENT_TYPE, CUSTOM_EVENT_PROPERTIES)
        }
        runOnMainSync {
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                routeOptions,
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        @RouterOrigin routerOrigin: String,
                    ) {
                        mapboxNavigation.setNavigationRoutes(routes)
                        mockLocationReplayerRule.playRoute(routes[0].directionsRoute)
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions,
                    ) {
                        // no impl
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        @RouterOrigin routerOrigin: String,
                    ) {
                        // no impl
                    }
                },
            )
        }

        Espresso.onIdle()
        routeCompleteIdlingResource.unregister()

        val filePath = runBlocking { mapboxNavigation.historyRecorder.stopRecording() }

        assertNotNull(filePath)
        verifyHistoryEvents(filePath!!, mockRoute, routeOptions)
    }

    @Test
    fun verify_history_files_are_recorded_and_readable_with_silent_waypoints() {
        // prepare
        val mockRoute = RoutesProvider.dc_very_short_two_legs_with_silent_waypoint(context)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        routeCompleteIdlingResource.register()

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(context)
            .baseUrl(mockWebServerRule.baseUrl)
            .coordinatesList(mockRoute.routeWaypoints)
            .waypointIndicesList(listOf(0, 2))
            .build()

        // execute
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mapboxNavigation.historyRecorder.pushHistory(CUSTOM_EVENT_TYPE, CUSTOM_EVENT_PROPERTIES)
        }
        runOnMainSync {
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                routeOptions,
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        @RouterOrigin routerOrigin: String,
                    ) {
                        mapboxNavigation.setNavigationRoutes(routes)
                        mockLocationReplayerRule.playRoute(routes[0].directionsRoute)
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions,
                    ) {
                        // no impl
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        @RouterOrigin routerOrigin: String,
                    ) {
                        // no impl
                    }
                },
            )
        }

        Espresso.onIdle()
        routeCompleteIdlingResource.unregister()

        val filePath = runBlocking { mapboxNavigation.historyRecorder.stopRecording() }

        assertNotNull(filePath)
        verifyHistoryEvents(filePath!!, mockRoute, routeOptions)
    }

    private fun verifyHistoryEvents(
        filePath: String,
        mockRoute: MockRoute,
        routeOptions: RouteOptions,
    ) {
        val historyReader = MapboxHistoryReader(filePath)

        // Verify hasNext
        assertTrue(historyReader.hasNext())

        // Verify we can read until end of file
        val historyEvents = historyReader.asSequence().toList()
        assertTrue("${historyEvents.size} > 10", historyEvents.size > 10)

        // Verify the custom event
        val customEvent = historyEvents
            .find { it is HistoryEventPushHistoryRecord } as HistoryEventPushHistoryRecord
        assertEquals(customEvent.type, CUSTOM_EVENT_TYPE)
        assertEquals(customEvent.properties, CUSTOM_EVENT_PROPERTIES)

        // Verify the first location
        val firstLocation = historyEvents
            .find { it is HistoryEventUpdateLocation } as HistoryEventUpdateLocation
        assertEquals(
            mockRoute.routeWaypoints.first().longitude(),
            firstLocation.location.longitude,
            0.0001,
        )
        assertEquals(
            mockRoute.routeWaypoints.first().latitude(),
            firstLocation.location.latitude,
            0.0001,
        )

        // Verify the set route event
        val setRouteEvent = historyEvents.find {
            it is HistoryEventSetRoute && it.navigationRoute != null
        } as HistoryEventSetRoute
        assertEquals(
            mockRoute.routeResponse.routes().first().duration(),
            setRouteEvent.navigationRoute!!.directionsRoute.duration(),
            0.001,
        )
        assertEquals(setRouteEvent.legIndex, 0)
        assertEquals(setRouteEvent.routeIndex, 0)
        assertEquals(
            routeOptions.geometries(),
            setRouteEvent.geometries,
        )
        assertEquals(
            routeOptions.profile(),
            setRouteEvent.profile,
        )
        val waypointIndices = routeOptions.waypointIndicesList()
        if (waypointIndices == null) {
            assertTrue(setRouteEvent.waypoints.none { it.isSilent })
        } else {
            setRouteEvent.waypoints.forEachIndexed { index, waypoint ->
                if (waypointIndices.contains(index)) {
                    assertFalse(
                        "waypoint at index $index shouldn't be silent",
                        waypoint.isSilent,
                    )
                } else {
                    assertTrue(
                        "waypoint at index $index should be silent",
                        waypoint.isSilent,
                    )
                }
            }
        }

        // Verify get status event as non-zero timestamp
        val getStatus = historyEvents
            .find { it is HistoryEventGetStatus } as HistoryEventGetStatus
        assertTrue(getStatus.elapsedRealtimeNanos > 0)
    }

    @Test
    fun legacy_json_verify_set_route_works_for_clearing_a_route() {
        val historyReader = historyReaderFromAssetFile("set_route_event_cleared.json")

        val events: List<HistoryEvent> = historyReader.asSequence().toList()

        assertEquals(1, events.size)
        val setRouteEvent = events[0] as HistoryEventSetRoute
        assertNull(setRouteEvent.navigationRoute)
        assertEquals(0, setRouteEvent.routeIndex)
        assertEquals(0, setRouteEvent.legIndex)
    }

    @Test(expected = Throwable::class)
    fun legacy_json_verify_invalid_json_crashes() {
        val historyReader = historyReaderFromAssetFile("set_route_event_invalid.json")

        historyReader.asSequence().toList()
    }

    @Test
    fun verify_identifying_events_can_be_found() = sdkTest {
        mapboxNavigation.startTripSession()
        val firstJson = """{"identifier":"first"}"""
        val secondJson = """{"identifier":"second"}"""
        val thirdJson = """{"identifier":"third"}"""

        val firstFile = startAndStopRecording(firstJson)
        val secondFile = startAndStopRecording(secondJson)
        val thirdFile = startAndStopRecording(thirdJson)

        fun String.findCustomEvent() = MapboxHistoryReader(this).asSequence()
            .filterIsInstance(HistoryEventPushHistoryRecord::class.java)
            .last()
        assertEquals(firstJson, firstFile.findCustomEvent().properties)
        assertEquals(secondJson, secondFile.findCustomEvent().properties)
        assertEquals(thirdJson, thirdFile.findCustomEvent().properties)
    }

    private suspend fun startAndStopRecording(eventJson: String): String {
        mapboxNavigation.historyRecorder.startRecording()
        mapboxNavigation.historyRecorder.pushHistory(CUSTOM_EVENT_TYPE, eventJson)
        return mapboxNavigation.historyRecorder.stopRecording()!!
    }

    private fun historyReaderFromAssetFile(
        name: String,
    ): MapboxHistoryReader {
        val inputStream: InputStream = context.assets.open(name)
        val outputFile = File(testDirectory, name)
        outputFile.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        return MapboxHistoryReader(outputFile.absolutePath)
    }

    private companion object {
        private const val CUSTOM_EVENT_TYPE = "custom_event_type"
        private const val CUSTOM_EVENT_PROPERTIES = """
            {"name":"John", "age":30, "car":null}
        """
    }
}
