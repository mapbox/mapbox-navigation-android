package com.mapbox.navigation.instrumentation_tests.core

import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.replay.route.ReplayRouteOptions
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.FirstLocationIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.IdlingPolicyTimeoutRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteAlternativesIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteRequestIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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
        .apply {
            mapper.options = ReplayRouteOptions.Builder()
                .gpsNoiseMeters(5.0)
                .build()
        }

    @get:Rule
    val idlingPolicyRule = IdlingPolicyTimeoutRule(40, TimeUnit.SECONDS)

    @get:Rule
    val historyTestRule = MapboxHistoryTestRule()

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setup() {
        mockLocationUpdatesRule.pushLocationUpdate {
            latitude = 38.56301
            longitude = -121.46685
        }

        runOnMainSync {
            val routeAlternativesOptions = RouteAlternativesOptions.Builder()
                .intervalMillis(TimeUnit.SECONDS.toMillis(10))
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
    fun expect_faster_route_alternatives() {
        // Prepare with a slow alternative route.
        val coordinates = listOf(
            Point.fromLngLat(-121.46685, 38.56301),
            Point.fromLngLat(-121.445697, 38.56707)
        )
        setupMockRequestHandlers(coordinates)
        val navigationRoute = requestDirectionsRouteSync(coordinates)
        val reversed = navigationRoute.copy(
            routesResponse = navigationRoute.routesResponse!!.toBuilder()
                .routes(navigationRoute.routesResponse!!.routes().reversed())
                .build()
        )


        // Start playing locations and wait for an enhanced location.
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mockLocationReplayerRule.playRoute(reversed.primaryRoute()!!)
            mapboxNavigation.startTripSession()
        }

//        // TODO provide a history file to see why this is not working
//        val firstLocationIdlingResource = FirstLocationIdlingResource(mapboxNavigation)
//        firstLocationIdlingResource.firstLocationSync()

        // Simulate a driver on the slow route.
        runOnMainSync {
            mapboxNavigation.setRoutes(reversed)
        }

        // Wait for route alternatives to be found.
        val alternativesIdlingResource = RouteAlternativesIdlingResource(mapboxNavigation)
        alternativesIdlingResource.register()
        mapboxHistoryTestRule.stopRecordingOnCrash("no route alternatives") {
            Espresso.onIdle()
        }

        // Verify faster alternatives are found.
        val alternatives = alternativesIdlingResource.alternatives!!
        assertTrue(alternatives.isNotEmpty())
        val durationRemaining = alternativesIdlingResource.routeProgress!!.durationRemaining
        val alternativeRouteDuration = alternatives.first().duration()
        assertTrue(alternativeRouteDuration < durationRemaining)
    }

    private fun setupMockRequestHandlers(coordinates: List<Point>) {
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_alternative_start),
                coordinates
            )
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_alternative_during_navigation),
                null
            )
        )
    }

    private fun requestDirectionsRouteSync(coordinates: List<Point>): NavigationRoute {
        val routeOptions = RouteOptions.builder().applyDefaultNavigationOptions()
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .build()
        val routeRequestIdlingResource = RouteRequestIdlingResource(mapboxNavigation, routeOptions)
        return routeRequestIdlingResource.requestRoutesSync()
    }
}
