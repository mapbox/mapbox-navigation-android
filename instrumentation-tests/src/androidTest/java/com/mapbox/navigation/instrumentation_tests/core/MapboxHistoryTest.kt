package com.mapbox.navigation.instrumentation_tests.core

import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
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
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch

class MapboxHistoryTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeCompleteIdlingResource: RouteProgressStateIdlingResource
    private lateinit var testDirectory: File

    @Before
    fun createTestDirectory() {
        testDirectory = File(activity.filesDir, "mapbox_history_test_directory")
            .also { it.mkdirs() }
    }

    @After
    fun deleteTestDirectory() {
        testDirectory.deleteRecursively()
    }

    @Before
    fun setup() {
        Espresso.onIdle()

        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .historyRecorderOptions(
                    HistoryRecorderOptions.Builder()
                        .build()
                )
                .build()
        )
        mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
        routeCompleteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.COMPLETE
        )
    }

    @Test
    fun verify_history_files_are_recorded_and_readable() {
        // prepare
        val mockRoute = MockRoutesProvider.dc_very_short(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        routeCompleteIdlingResource.register()

        // execute
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mapboxNavigation.historyRecorder.pushHistory(CUSTOM_EVENT_TYPE, CUSTOM_EVENT_PROPERTIES)

            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = mockRoute.routeWaypoints.first().latitude()
                longitude = mockRoute.routeWaypoints.first().longitude()
            }
        }
        runOnMainSync {
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(activity)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .coordinatesList(mockRoute.routeWaypoints).build(),
                object : RouterCallback {
                    override fun onRoutesReady(
                        routes: List<DirectionsRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        mapboxNavigation.setRoutes(routes)
                        mockLocationReplayerRule.playRoute(routes[0])
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions
                    ) {
                        // no impl
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        routerOrigin: RouterOrigin
                    ) {
                        // no impl
                    }
                }
            )
        }

        // assert and clean up
        Espresso.onIdle()
        routeCompleteIdlingResource.unregister()

        runOnMainSync {
            val countDownLatch = CountDownLatch(1)
            mapboxNavigation.historyRecorder.stopRecording { filePath ->
                assertNotNull(filePath)
                verifyHistoryEvents(filePath!!)
                countDownLatch.countDown()
            }
            countDownLatch.await()
        }
    }

    private fun verifyHistoryEvents(filePath: String) {
        val historyReader = MapboxHistoryReader(filePath)

        // Verify hasNext
        assertTrue(historyReader.hasNext())

        // Verify we can read until end of file
        val historyEvents = historyReader.asSequence().toList()
        assertTrue(historyEvents.size > 10)

        // Verify the custom event
        val customEvent = historyEvents
            .find { it is HistoryEventPushHistoryRecord } as HistoryEventPushHistoryRecord
        assertEquals(customEvent.type, CUSTOM_EVENT_TYPE)
        assertEquals(customEvent.properties, CUSTOM_EVENT_PROPERTIES)

        // Verify the first location
        val firstLocation = historyEvents
            .find { it is HistoryEventUpdateLocation } as HistoryEventUpdateLocation
        assertEquals(firstLocation.location.longitude, -77.031991, 0.00001)
        assertEquals(firstLocation.location.latitude, 38.894721, 0.00001)

        // Verify the set route event
        val setRouteEvent = historyEvents
            .find { it is HistoryEventSetRoute } as HistoryEventSetRoute
        val directionsRoute = DirectionsRoute
            .fromJson(setRouteEvent.directionsRoute!!, TEST_ACCESS_TOKEN)
        assertEquals(24.001, directionsRoute.duration(), 0.001)
        assertEquals(setRouteEvent.legIndex, 0)
        assertEquals(setRouteEvent.routeIndex, 0)
        assertEquals(DirectionsCriteria.GEOMETRY_POLYLINE6, setRouteEvent.geometries)
        assertEquals(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, setRouteEvent.profile)

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
        assertNull(setRouteEvent.directionsRoute)
        assertEquals(0, setRouteEvent.routeIndex)
        assertEquals(0, setRouteEvent.legIndex)
    }

    @Test(expected = Exception::class)
    fun legacy_json_verify_invalid_json_crashes() {
        val historyReader = historyReaderFromAssetFile("set_route_event_invalid.json")

        val events: List<HistoryEvent> = historyReader.asSequence().toList()

        assertEquals(1, events.size)
        val setRouteEvent = events[0] as HistoryEventSetRoute
        assertNotNull(setRouteEvent.directionsRoute)
        DirectionsRoute.fromJson(setRouteEvent.directionsRoute!!, TEST_ACCESS_TOKEN)
    }

    @Test
    fun legacy_json_verify_reading_valid_json() {
        val historyReader = historyReaderFromAssetFile("set_route_event_valid.json")

        val events: List<HistoryEvent> = historyReader.asSequence().toList()

        assertEquals(1, events.size)
        val setRouteEvent = events[0] as HistoryEventSetRoute
        assertNotNull(setRouteEvent.directionsRoute)
        val directionsRoute = DirectionsRoute
            .fromJson(setRouteEvent.directionsRoute!!, TEST_ACCESS_TOKEN)
        assertEquals(821.8, directionsRoute.distance(), 0.1)
        assertEquals(157.0, directionsRoute.duration(), 0.1)
        assertEquals(0, setRouteEvent.routeIndex)
        assertEquals(0, setRouteEvent.legIndex)
        assertEquals(DirectionsCriteria.GEOMETRY_POLYLINE6, setRouteEvent.geometries)
        assertEquals(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, setRouteEvent.profile)
    }

    private fun historyReaderFromAssetFile(
        name: String
    ): MapboxHistoryReader {
        val inputStream: InputStream = activity.assets.open(name)
        val outputFile = File(testDirectory, name)
        outputFile.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        return MapboxHistoryReader(outputFile.absolutePath)
    }

    private companion object {
        private const val CUSTOM_EVENT_TYPE = "custom_event_type"
        private const val CUSTOM_EVENT_PROPERTIES = "custom_event_properties"
        private const val TEST_ACCESS_TOKEN = "pk.test-access-token"
    }
}
