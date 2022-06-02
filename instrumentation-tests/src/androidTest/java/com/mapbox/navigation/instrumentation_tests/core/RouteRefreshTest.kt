package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockRoutingTileEndpointErrorRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.IdlingPolicyTimeoutRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteRequestIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RoutesObserverIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class RouteRefreshTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    private companion object {
        private const val LOG_CATEGORY = "RouteRefreshTest"
    }

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val idlingPolicyRule = IdlingPolicyTimeoutRule(35, TimeUnit.SECONDS)

    private lateinit var mapboxNavigation: MapboxNavigation
    private val coordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674)
    )

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = coordinates[0].latitude()
        longitude = coordinates[0].longitude()
        bearing = 190f
    }

    @Before
    fun setup() {
        setupMockRequestHandlers(coordinates)

        runOnMainSync {
            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                .build()
            RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
                isAccessible = true
                set(routeRefreshOptions, 3_000L)
            }
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .routeRefreshOptions(routeRefreshOptions)
                    .routingTilesOptions(
                        RoutingTilesOptions.Builder()
                            .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                            .build()
                    )
                    .navigatorPredictionMillis(0L)
                    .build()
            )
        }
    }

    @Test
    fun expect_route_refresh_to_update_traffic_annotations_and_incidents() {
        // Request a route.
        val routes = requestDirectionsRouteSync(coordinates).reversed()

        // Create an observer resource that captures the routes.
        val initialRouteIdlingResource = RoutesObserverIdlingResource(mapboxNavigation)
            .register()

        // Set navigation with the route.
        runOnMainSync {
            mapboxNavigation.setRoutes(routes)
            mapboxNavigation.startTripSession()
            mockLocationReplayerRule.loopUpdate(
                mockLocationUpdatesRule.generateLocationUpdate {
                    latitude = coordinates[0].latitude()
                    longitude = coordinates[0].longitude()
                    bearing = 190f
                },
                times = 60
            )
            mapboxNavigation.registerRouteProgressObserver { routeProgress ->
                logD(
                    "progress state: ${routeProgress.currentState}",
                    LOG_CATEGORY
                )
                logD(
                    "progress duration remaining: ${routeProgress.durationRemaining}",
                    LOG_CATEGORY
                )
            }
        }

        // Wait for the initial route.
        val initialRoutes = initialRouteIdlingResource.next()

        // Wait for the route refresh.
        val refreshedRoutes = initialRouteIdlingResource.next()
        initialRouteIdlingResource.unregister()

        val progressIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.TRACKING
        )
        progressIdlingResource.register()
        Espresso.onIdle()
        progressIdlingResource.unregister()

        val latch = CountDownLatch(2)
        runOnMainSync {
            mapboxNavigation.registerRouteProgressObserver { routeProgress ->
                if (isRefreshedRouteDistance(routeProgress)) {
                    latch.countDown()
                }
            }
            mapboxNavigation.registerRoadObjectsOnRouteObserver { upcomingRoadObjects ->
                // upcoming road's objects mut be exact 2 with following ids
                if (upcomingRoadObjects.size == 2 &&
                    upcomingRoadObjects.map { it.roadObject.id }
                        .containsAll(listOf("11589180127444257", "14158569638505033"))
                ) {
                    latch.countDown()
                }
            }
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw AssertionError(
                """progress duration remaining or upcoming road objects weren't  
                    refreshed by native navigator
                """.trimMargin()
            )
        }

        // Only the annotations AND incidents are refreshed. So sum up the duration from the old and new.
        val sanityLeg = routes.first().legs()!!
        val sanityDuration = sanityLeg.first().annotation()?.duration()?.sum()!!
        val initialLeg = initialRoutes.first().legs()?.first()!!
        val initialDuration = initialLeg.annotation()?.duration()?.sum()!!
        val refreshedLeg = refreshedRoutes.first().legs()?.first()!!
        val refreshedDuration = refreshedLeg.annotation()?.duration()?.sum()!!

        assertEquals(1, initialLeg.incidents()!!.size)
        assertEquals("11589180127444257", initialLeg.incidents()!!.first().id())
        assertEquals(2, refreshedLeg.incidents()!!.size)
        assertTrue(
            refreshedLeg.incidents()!!.map { it.id() }
                .containsAll(listOf("11589180127444257", "14158569638505033"))
        )

        assertEquals(sanityDuration, initialDuration, 0.0)
        assertEquals(227.918, initialDuration, 0.0001)
        assertEquals(1180.651, refreshedDuration, 0.0001)
    }

    @Test
    fun routeRefreshesAfterCleanup() = sdkTest {
        val routeOptions = generateRouteOptions(coordinates)
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()

        waitForRouteToRefresh()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf())
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        waitForRouteToRefresh()
    }

    private fun stayOnInitialPosition() {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = coordinates[0].latitude()
                longitude = coordinates[0].longitude()
                bearing = 190f
            },
            times = 120
        )
    }

    private suspend fun waitForRouteToRefresh(): RouteProgress =
        mapboxNavigation.routeProgressUpdates()
            .filter { isRefreshedRouteDistance(it) }
            .first()

    private fun isRefreshedRouteDistance(it: RouteProgress): Boolean {
        val expectedDurationRemaining = 1180.651
        // 30 seconds margin of error
        return (it.durationRemaining - expectedDurationRemaining).absoluteValue < 30
    }

    // Will be ignored when .baseUrl(mockWebServerRule.baseUrl) is commented out
    // in the requestDirectionsRouteSync function.
    private fun setupMockRequestHandlers(coordinates: List<Point>) {
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_route_refresh),
                coordinates
            )
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh",
                readRawFileText(activity, R.raw.route_response_route_refresh_annotations)
            )
        )
        mockWebServerRule.requestHandlers.add(
            MockRoutingTileEndpointErrorRequestHandler()
        )
    }

    private fun requestDirectionsRouteSync(coordinates: List<Point>): List<DirectionsRoute> {
        val routeOptions = generateRouteOptions(coordinates)
        val routeRequestIdlingResource = RouteRequestIdlingResource(mapboxNavigation, routeOptions)
        return routeRequestIdlingResource.requestRoutesSync()
    }

    private fun generateRouteOptions(coordinates: List<Point>): RouteOptions {
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .build()
    }
}
