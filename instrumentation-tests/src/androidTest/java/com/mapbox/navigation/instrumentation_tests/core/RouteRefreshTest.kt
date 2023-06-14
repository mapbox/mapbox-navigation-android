package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.roadObjectsOnRoute
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routesUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockRoutingTileEndpointErrorRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.IdlingPolicyTimeoutRule
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class RouteRefreshTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

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

    private lateinit var failByRequestRouteRefreshResponse: FailByRequestMockRequestHandler

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
                    .navigatorPredictionMillis(0L)
                    .build()
            )
        }
    }

    @Test
    fun expect_route_refresh_to_update_traffic_annotations_and_incidents_for_all_routes() =
        sdkTest {
            val routeOptions = generateRouteOptions(coordinates)
            val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
                .reversed()

            mapboxNavigation.setNavigationRoutes(requestedRoutes)
            mapboxNavigation.startTripSession()
            stayOnInitialPosition()
            val routeUpdates = mapboxNavigation.routesUpdates()
                .take(2)
                .map { it.navigationRoutes }
                .toList()
            val initialRoutes = routeUpdates[0]
            val refreshedRoutes = routeUpdates[1]

            mapboxNavigation.routeProgressUpdates()
                .filter { routeProgress -> isRefreshedRouteDistance(routeProgress) }
                .first()
            mapboxNavigation.roadObjectsOnRoute()
                .filter { upcomingRoadObjects ->
                    upcomingRoadObjects.size == 2 &&
                        upcomingRoadObjects.map { it.roadObject.id }
                            .containsAll(listOf("11589180127444257", "14158569638505033"))
                }
                .first()

            assertEquals(
                "the test works only with 2 routes",
                2,
                requestedRoutes.size
            )
            assertEquals(
                listOf("11589180127444257"),
                initialRoutes[0].getIncidentsIdFromTheRoute(0)
            )
            assertEquals(
                listOf("11589180127444257", "14158569638505033").sorted(),
                refreshedRoutes[0].getIncidentsIdFromTheRoute(0)?.sorted()
            )
            assertEquals(
                listOf("11589180127444257"),
                initialRoutes[1].getIncidentsIdFromTheRoute(0)
            )
            assertEquals(
                listOf("11589180127444257", "14158569638505033").sorted(),
                refreshedRoutes[1].getIncidentsIdFromTheRoute(0)?.sorted()
            )

            assertEquals(
                "initial should be the same as requested",
                requestedRoutes[0].getDurationAnnotationsFromLeg(0),
                initialRoutes[0].getDurationAnnotationsFromLeg(0)
            )
            assertEquals(227.918, initialRoutes[0].getDurationOfLeg(0), 0.0001)
            assertEquals(1189.651, refreshedRoutes[0].getDurationOfLeg(0), 0.0001)

            assertEquals(
                requestedRoutes[1].getDurationOfLeg(0),
                initialRoutes[1].getDurationOfLeg(0),
                0.0
            )
            assertEquals(224.2239, initialRoutes[1].getDurationOfLeg(0), 0.0001)
            assertEquals(1189.651, refreshedRoutes[1].getDurationOfLeg(0), 0.0001)
        }

    @Test
    fun routeRefreshesWorksAfterSettingsNewRoutes() = sdkTest {
        val routeOptions = generateRouteOptions(coordinates)
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()

        waitForRouteToSuccessfullyRefresh()
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        waitForRouteToSuccessfullyRefresh()
    }

    @Test
    fun routeSuccessfullyRefreshesAfterInvalidationOfExpiringData() = sdkTest {
        val routeOptions = generateRouteOptions(coordinates)
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        failByRequestRouteRefreshResponse.failResponse = true
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        // act
        val refreshedRoutes = mapboxNavigation.routesUpdates()
            .filter { it.reason == ROUTES_UPDATE_REASON_REFRESH }
            .map { it.navigationRoutes }
            .first()
        val refreshedRouteCongestions = refreshedRoutes
            .first()
            .directionsRoute
            .legs()
            ?.firstOrNull()
            ?.annotation()
            ?.congestion()
        assertTrue(
            "expected unknown congestions, but they were $refreshedRouteCongestions",
            refreshedRouteCongestions?.all { it == "unknown" } ?: false
        )
        failByRequestRouteRefreshResponse.failResponse = false
        waitForRouteToSuccessfullyRefresh()
    }

    @Test
    fun routeAlternativeMetadataUpdatedAlongWithRouteRefresh() = sdkTest {
        val routeOptions = generateRouteOptions(coordinates)
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        val alternativesMetadataLegacy = mapboxNavigation.getAlternativeMetadataFor(routes).first()
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.routesUpdates()
            .first { it.reason == ROUTES_UPDATE_REASON_REFRESH }

        val alternativesMetadata = mapboxNavigation.getAlternativeMetadataFor(
            mapboxNavigation.getNavigationRoutes()
        ).first()

        assertNotNull(alternativesMetadataLegacy)
        assertNotNull(alternativesMetadata)
        assertEquals(
            alternativesMetadataLegacy.navigationRoute.id,
            alternativesMetadata.navigationRoute.id
        )
        assertNotEquals(alternativesMetadataLegacy, alternativesMetadata)
        assertEquals(266.0, alternativesMetadataLegacy.infoFromStartOfPrimary.duration, 1.0)
        assertEquals(275.0, alternativesMetadata.infoFromStartOfPrimary.duration, 1.0)
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

    private suspend fun waitForRouteToSuccessfullyRefresh(): RouteProgress =
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
        failByRequestRouteRefreshResponse = FailByRequestMockRequestHandler(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh",
                readRawFileText(activity, R.raw.route_response_route_refresh_annotations)
            )
        )
        mockWebServerRule.requestHandlers.add(failByRequestRouteRefreshResponse)
        mockWebServerRule.requestHandlers.add(
            MockRoutingTileEndpointErrorRequestHandler()
        )
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

private fun NavigationRoute.getDurationOfLeg(legIndex: Int): Double =
    getDurationAnnotationsFromLeg(legIndex)
        ?.sum()!!

private fun NavigationRoute.getDurationAnnotationsFromLeg(legIndex: Int): List<Double>? =
    directionsRoute.legs()?.get(legIndex)
        ?.annotation()
        ?.duration()

private fun NavigationRoute.getIncidentsIdFromTheRoute(legIndex: Int): List<String>? =
    directionsRoute.legs()?.get(legIndex)
        ?.incidents()
        ?.map { it.id() }
