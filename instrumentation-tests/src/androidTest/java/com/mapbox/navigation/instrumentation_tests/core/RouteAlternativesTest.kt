package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.FilteredRouteAlternativesIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.FirstLocationIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteAlternativesIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteRequestIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.utils.internal.logE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * This test ensures that alternative route recommendations
 * are given during active guidance.
 */
class RouteAlternativesTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private val coordinates = listOf(
        Point.fromLngLat(-122.2750659, 37.8052036),
        Point.fromLngLat(-122.2647245, 37.8138895)
    )

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = coordinates[0].latitude()
        longitude = coordinates[0].longitude()
    }

    @Before
    fun setup() {
        runOnMainSync {
            val routeAlternativesOptions = RouteAlternativesOptions.Builder()
                .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                .build()
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .routeAlternativesOptions(routeAlternativesOptions)
                    .build()
            )
            mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
        }
    }

    @Test
    fun expect_initial_alternative_route_removed_after_navigating_past() {
        // Prepare with alternative routes.
        setupMockRequestHandlers(coordinates)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_alternative_during_navigation),
                coordinates,
                relaxedExpectedCoordinates = true
            )
        )
        val routes = requestDirectionsRouteSync(coordinates)

        // Play the slower alternative route.
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mockLocationReplayerRule.playRoute(routes.first())
            mapboxNavigation.startTripSession()
        }

        // Wait for enhanced locations to start and then set the routes.
        val firstLocationIdlingResource = FirstLocationIdlingResource(mapboxNavigation)
        firstLocationIdlingResource.firstLocationSync()
        runOnMainSync {
            mapboxNavigation.setRoutes(routes)
        }

        // The alternative route is missed, so we expect the route to be removed.
        val firstAlternative = RouteAlternativesIdlingResource(mapboxNavigation)
        firstAlternative.register()
        mapboxHistoryTestRule.stopRecordingOnCrash("alternatives failed") {
            Espresso.onIdle()
        }
        firstAlternative.unregister()
        assertTrue(firstAlternative.calledOnMainThread)

        val countDownLatch = CountDownLatch(1)
        runOnMainSync {
            mapboxNavigation.historyRecorder.stopRecording {
                logE("history path=$it", "DEBUG")
                countDownLatch.countDown()
            }
        }
        countDownLatch.await()

        // Verify alternative routes events were triggered.
        assertEquals(2, routes.size)
        assertTrue(mapboxNavigation.getRoutes().isNotEmpty())
        assertNotNull(firstAlternative.routeProgress)
        assertNotNull(firstAlternative.alternatives)
    }

    @Test
    fun alternative_requests_use_original_route_base_url() {
        setupMockRequestHandlers(coordinates)
        val routes = requestDirectionsRouteSync(coordinates)

        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_alternative_during_navigation),
                coordinates,
                relaxedExpectedCoordinates = true
            )
        )
        // Play the slower alternative route.
        mapboxNavigation.historyRecorder.startRecording()
        mockLocationReplayerRule.playRoute(routes.first())
        mapboxNavigation.startTripSession()

        // Wait for enhanced locations to start and then set the routes.
        val firstLocationIdlingResource = FirstLocationIdlingResource(mapboxNavigation)
        firstLocationIdlingResource.firstLocationSync()
        mapboxNavigation.setRoutes(routes)
        // receive first alternative update, which contains the original route

        val firstAlternative =
            FilteredRouteAlternativesIdlingResource(mapboxNavigation) { alternatives ->
                alternatives.isNotEmpty() && alternatives.none {
                    it.requestUuid() == "1SSd29ZxmjD7ELLqDJHRPPDP5W4wdh633IbGo41pJrL6wpJRmzNaMA=="
                }
            }
        firstAlternative.register()
        mapboxHistoryTestRule.stopRecordingOnCrash("alternatives failed") {
            Espresso.onIdle()
        }
        firstAlternative.unregister()

        assertEquals(
            "DD8MJ37zcI2gU4XXhtt-Gz1vdFShCMtf7AOyEHVylhqcEyreYNiT6Q==",
            firstAlternative.alternatives?.firstOrNull()?.requestUuid()
        )
    }

    private fun setupMockRequestHandlers(coordinates: List<Point>) {
        // Nav-native requests alternate routes, so we are only
        // ensuring the initial route has alternatives.
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_alternative_start),
                coordinates
            )
        )
    }

    private fun requestDirectionsRouteSync(coordinates: List<Point>): List<DirectionsRoute> {
        val routeOptions = RouteOptions.builder().applyDefaultNavigationOptions()
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .build()
        val routeRequestIdlingResource = RouteRequestIdlingResource(mapboxNavigation, routeOptions)
        return routeRequestIdlingResource.requestRoutesSync()
    }
}
