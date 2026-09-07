package com.mapbox.navigation.instrumentation_tests.ui.routeline

import android.location.Location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class AlternativeRouteSelectionTest : BaseTest<BasicNavigationViewActivity>(
    BasicNavigationViewActivity::class.java,
) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView

    override fun setupMockLocation(): Location {
        val origin = RoutesProvider.multiple_routes(context).routeWaypoints.first()
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
    }

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .build(),
            )
        }
    }

    @After
    fun tearDown() {
        MapboxNavigationProvider.destroy()
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
            val updatedRoutes = routeLineApi.getNavigationRoutes().toMutableList()
            val alternative = updatedRoutes[1]
            updatedRoutes[1] = updatedRoutes[0]
            updatedRoutes[0] = alternative

            // Update the route line api and MapboxNavigation with an alternative primary.
            routeLineApi.setNavigationRoutes(updatedRoutes) { result ->
                assertTrue(result.isValue)

                routeLineView.renderRouteDrawData(
                    activity.mapboxMap.getStyle()!!,
                    result,
                )
                assertEquals(alternative, routeLineApi.getNavigationRoutes()[0])
                assertEquals(alternative, routeLineApi.getPrimaryNavigationRoute())
                routeLineRoutesIsSet.countDown()
            }
            mapboxNavigation.setNavigationRoutes(updatedRoutes)

            // Observe route progress and verify the alternative is now the primary route.
            mapboxNavigation.registerRouteProgressObserver { routeProgress ->
                // The route index has been changed, so only compare the geometry
                assertEquals(alternative.directionsRoute.geometry(), routeProgress.route.geometry())
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
        runBlocking(Dispatchers.Main) {
            val routes = mapboxNavigation.requestMockRoutes(
                mockWebServerRule,
                RoutesProvider.multiple_routes(context),
            )
            mapboxNavigation.setNavigationRoutes(
                routes,
            )
            mockLocationReplayerRule.playRoute(routes.first().directionsRoute)
        }
    }

    private fun verifyRouteLineIsUpdatedWithAlternatives() {
        val countDownLatch = CountDownLatch(1)
        runOnMainSync {
            routeLineView = MapboxRouteLineView(
                MapboxRouteLineViewOptions.Builder(activity).build(),
            )
            routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())

            mapboxNavigation.registerRoutesObserver(
                object : RoutesObserver {
                    override fun onRoutesChanged(result: RoutesUpdatedResult) {
                        mapboxNavigation.unregisterRoutesObserver(this)

                        routeLineApi.setNavigationRoutes(result.navigationRoutes) { setResult ->
                            routeLineView.renderRouteDrawData(
                                activity.mapboxMap.getStyle()!!,
                                setResult,
                            )
                        }

                        assertEquals(3, result.navigationRoutes.size)
                        countDownLatch.countDown()
                    }
                },
            )
        }
        countDownLatch.await()
    }
}
