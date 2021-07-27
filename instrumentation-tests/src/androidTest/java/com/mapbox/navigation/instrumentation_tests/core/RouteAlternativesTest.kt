package com.mapbox.navigation.instrumentation_tests.core

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
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.FirstLocationIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteAlternativesIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteRequestIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * This test ensures that alternative route recommendations
 * are given during active guidance.
 */
// TODO: Refactor https://github.com/mapbox/mapbox-navigation-android/issues/4429
class RouteAlternativesTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setup() {
        val routeAlternativesOptions = RouteAlternativesOptions.Builder()
            .intervalMillis(TimeUnit.SECONDS.toMillis(10))
            .build()
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .routeAlternativesOptions(routeAlternativesOptions)
                .build()
        )
    }

    @Test
    fun expect_faster_route_alternatives() {
        // Prepare with a slow alternative route.
        val coordinates = listOf(
            Point.fromLngLat(-121.46685, 38.56301),
            Point.fromLngLat(-121.445697, 38.56707)
        )
        setupMockRequestHandlers(coordinates)
        val routes = requestDirectionsRouteSync(coordinates).reversed()

        // Start playing locations and wait for an enhanced location.
        runOnMainSync {
            mockLocationReplayerRule.playRoute(routes.first())
            mapboxNavigation.startTripSession()
        }
        val firstLocationIdlingResource = FirstLocationIdlingResource(mapboxNavigation)
        firstLocationIdlingResource.firstLocationSync()

        // Simulate a driver on the slow route.
        runOnMainSync {
            mapboxNavigation.setRoutes(routes)
        }

        // Wait for route alternatives to be found.
        val alternativesIdlingResource = RouteAlternativesIdlingResource(mapboxNavigation)
        alternativesIdlingResource.register()
        Espresso.onIdle()

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
