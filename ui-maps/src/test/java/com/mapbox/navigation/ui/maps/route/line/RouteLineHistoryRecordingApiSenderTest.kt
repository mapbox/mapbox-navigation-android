package com.mapbox.navigation.ui.maps.route.line

import com.google.gson.JsonParser
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.HistoryRecordingEnabledObserver
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingEnabledObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.util.MutexBasedScope
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class RouteLineHistoryRecordingApiSenderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val recorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    private lateinit var pusher: RouteLineHistoryRecordingPusher
    private lateinit var sender: RouteLineHistoryRecordingApiSender

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationProvider)
        pusher = RouteLineHistoryRecordingPusher(
            coroutineRule.testDispatcher,
            MutexBasedScope(coroutineRule.coroutineScope),
        )

        mockkObject(RouteLineHistoryRecordingPusherProvider)
        every { RouteLineHistoryRecordingPusherProvider.instance } returns pusher
        sender = RouteLineHistoryRecordingApiSender()
    }

    @After
    fun tearDown() {
        unmockkObject(RouteLineHistoryRecordingPusherProvider)
        unmockkObject(MapboxNavigationProvider)
    }

    @Test
    fun pushOptionsEventsIsAddedToQueue() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"options":{"lowCongestionRange":{"first":0,"last":39},"moderateCongestionRange":{"first":40,"last":59},"heavyCongestionRange":{"first":60,"last":79},"severeCongestionRange":{"first":80,"last":100},"trafficBackfillRoadClasses":["class1"],"calculateRestrictedRoadSections":true,"styleInactiveRouteLegsIndependently":true,"vanishingRouteLineEnabled":true,"vanishingRouteLineUpdateIntervalNano":123456,"isRouteCalloutsEnabled":false},"action":"options"},"subtype":"api","instanceId":"5bd24d9d-8368-4e04-8dd3-fd55fa5bf2d4"}"""
        /* ktlint-enable max-line-length */
        val options = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineUpdateIntervalNano(123456)
            .vanishingRouteLineEnabled(true)
            .calculateRestrictedRoadSections(true)
            .styleInactiveRouteLegsIndependently(true)
            .trafficBackfillRoadClasses(listOf("class1"))
            .build()
        sender.sendOptionsEvent(options)

        onRecorderEnabled()

        checkEvent(expected)
    }

    @Test
    fun pushSetRoutesEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"legIndex":2,"routeLines":[{"routeId":"route 1","featureId":"id1"},{"routeId":"route 2","featureId":"id2"}],"action":"set_routes"},"subtype":"api","instanceId":"e916efaf-d94f-4f47-b0c1-cdbcc9a2b5bd"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        val route1Id = "route 1"
        val route2Id = "route 2"
        val route1 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns route1Id
        }
        val route2 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns route2Id
        }
        sender.sendSetRoutesEvent(
            listOf(NavigationRouteLine(route1, "id1"), NavigationRouteLine(route2, "id2")),
            2,
        )

        checkEvent(expected)
    }

    @Test
    fun pushUpdateTraveledRouteLineEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"point":[3.4,1.3],"action":"update_traveled_route_line"},"subtype":"api","instanceId":"ede6008d-b4a7-4eaf-be71-d12e9f6a649b"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        sender.sendUpdateTraveledRouteLineEvent(Point.fromLngLat(3.4, 1.3))

        checkEvent(expected)
    }

    @Test
    fun pushClearRouteLineEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"action":"clear_route_line"},"subtype":"api","instanceId":"b2bb3f84-a786-47b0-9d7e-c47906cb74f1"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        sender.sendClearRouteLineEvent()

        checkEvent(expected)
    }

    @Test
    fun pushSetVanishingOffsetEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"offset":0.354,"action":"set_vanishing_offset"},"subtype":"api","instanceId":"9934170d-10aa-4b54-a02a-675dac9b329a"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        sender.sendSetVanishingOffsetEvent(0.354)

        checkEvent(expected)
    }

    @Test
    fun pushUpdateWithRouteProgressEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"routeId":"route#id","routeGeometryIndex":7,"state":"TRACKING","legIndex":2,"action":"update_with_route_progress"},"subtype":"api","instanceId":"5b21014f-00fc-468d-b3a4-b8a7770125c8"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        sender.sendUpdateWithRouteProgressEvent(
            mockk {
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 2
                }
                every { currentRouteGeometryIndex } returns 7
                every { currentState } returns RouteProgressState.TRACKING
                every { navigationRoute } returns mockk {
                    every { id } returns "route#id"
                }
            },
        )

        checkEvent(expected)
    }

    @Test
    fun pushCancelEvent() {
        /* ktlint-disable max-line-length */
        val expected =
            """{"value":{"action":"cancel"},"subtype":"api","instanceId":"b2bb3f84-a786-47b0-9d7e-c47906cb74f1"}"""
        /* ktlint-enable max-line-length */
        onRecorderEnabled()
        sender.sendCancelEvent()

        checkEvent(expected)
    }

    @Test
    fun sameInstanceIdIsUsed() {
        onRecorderEnabled()
        sender.sendCancelEvent()
        val id1 = getInstanceId()
        clearAllMocks(answers = false)
        sender.sendClearRouteLineEvent()
        val id2 = getInstanceId()

        assertEquals(id1, id2)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun onRecorderEnabled() {
        val observerSlot = slot<HistoryRecordingEnabledObserver>()
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { historyRecorder } returns recorder
            every { navigationOptions } returns mockk {
                every { copilotOptions } returns mockk {
                    every { shouldRecordRouteLineEvents } returns false
                }
            }
        }
        pusher.onAttached(mapboxNavigation)
        verify { mapboxNavigation.registerHistoryRecordingEnabledObserver(capture(observerSlot)) }
        observerSlot.captured.onEnabled(mockk(relaxed = true))
    }

    private fun checkEvent(expected: String) {
        val actualSlot = slot<String>()
        verify {
            recorder.pushHistory(
                "mbx.RouteLine",
                capture(actualSlot),
            )
        }
        val actualJson = JsonParser.parseString(actualSlot.captured).asJsonObject
        val expectedJson = JsonParser.parseString(expected).asJsonObject

        assertTrue(actualJson.getAsJsonPrimitive("instanceId").asString.isNotBlank())

        actualJson.remove("instanceId")
        expectedJson.remove("instanceId")

        assertEquals(expectedJson, actualJson)
    }

    private fun getInstanceId(): String {
        val actualSlot = slot<String>()
        verify {
            recorder.pushHistory(
                "mbx.RouteLine",
                capture(actualSlot),
            )
        }
        val actualJson = JsonParser.parseString(actualSlot.captured).asJsonObject
        return actualJson.getAsJsonPrimitive("instanceId").asString
    }
}
