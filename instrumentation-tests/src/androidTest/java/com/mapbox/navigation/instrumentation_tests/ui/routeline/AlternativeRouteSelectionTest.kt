package com.mapbox.navigation.instrumentation_tests.ui.routeline

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class AlternativeRouteSelectionTest : BaseTest<BasicNavigationViewActivity>(
    BasicNavigationViewActivity::class.java
) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView

    @Before
    fun setUp() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .build()
        )
    }

    @After
    fun tearDown() {
        mapboxNavigation.onDestroy()
    }

    @Test
    fun expect_route_apis_to_be_updated_with_alternatives() {
        // Initialize with a set of routes with alternatives. Later we will
        // update the routes with a new primary route and verify it is all set.
        setupRouteWithAlternatives()
        verifyRouteLineIsUpdatedWithAlternatives()

        // Wait for route progress to come back with an alternative set.
        val routeLineRoutesIsSet = CountDownLatch(1)
        val routeProgressCount = CountDownLatch(1)

        runOnMainSync {
            // Create routes where an alternative is set to the primary route position.
            val updatedRoutes = routeLineApi.getRoutes().toMutableList()
            val alternative = updatedRoutes[1]
            updatedRoutes[1] = updatedRoutes[0]
            updatedRoutes[0] = alternative

            // Update the route line api and MapboxNavigation with an alternative primary.
            val updatedRouteLines = updatedRoutes.map { RouteLine(it, null) }
            routeLineApi.setRoutes(updatedRouteLines) { result ->
                assertTrue(result.isValue)

                routeLineView.renderRouteDrawData(
                    activity.mapboxMap.getStyle()!!,
                    result
                )
                assertEquals(alternative, routeLineApi.getRoutes()[0])
                assertEquals(alternative, routeLineApi.getPrimaryRoute())
                routeLineRoutesIsSet.countDown()
            }
            mapboxNavigation.setRoutes(updatedRoutes)

            // Observe route progress and verify the alternative is now the primary route.
            mapboxNavigation.registerRouteProgressObserver { routeProgress ->
                assertEquals(alternative, routeProgress.route)
                routeProgressCount.countDown()
            }

            // Start the trip session and expect the route progress observer
            // above to have an alternative set as the primary route.
            mapboxNavigation.startTripSession()
        }

        routeLineRoutesIsSet.await()
        routeProgressCount.await()
    }

    private fun setupRouteWithAlternatives() {
        val directionsResponse = MockRoutesProvider
            .loadDirectionsResponse(activity, R.raw.multiple_routes)
        val origin = directionsResponse.waypoints()!!.map { it.location()!! }
            .first()
        val route = directionsResponse.routes()[0]
        runOnMainSync {
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = origin.latitude()
                longitude = origin.longitude()
            }
            mapboxNavigation.setRoutes(directionsResponse.routes())
            mockLocationReplayerRule.playRoute(route)
        }
    }

    private fun verifyRouteLineIsUpdatedWithAlternatives() {
        val countDownLatch = CountDownLatch(1)
        runOnMainSync {
            routeLineView = MapboxRouteLineView(MapboxRouteLineOptions.Builder(activity).build())
            routeLineApi = MapboxRouteLineApi(MapboxRouteLineOptions.Builder(activity).build())

            mapboxNavigation.registerRoutesObserver(object : RoutesObserver {
                override fun onRoutesChanged(routes: List<DirectionsRoute>) {
                    mapboxNavigation.unregisterRoutesObserver(this)

                    val routeLines = routes.map { RouteLine(it, null) }
                    routeLineApi.setRoutes(routeLines) { result ->
                        routeLineView.renderRouteDrawData(
                            activity.mapboxMap.getStyle()!!,
                            result
                        )
                    }

                    assertEquals(3, routes.size)
                    countDownLatch.countDown()
                }
            })
        }
        countDownLatch.await()
    }
}
