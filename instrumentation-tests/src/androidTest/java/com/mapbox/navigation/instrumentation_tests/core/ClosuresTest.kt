package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.route.deserializeNavigationRouteFrom
import com.mapbox.navigation.base.internal.route.serialize
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.base.utils.route.hasUnexpectedUpcomingClosures
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.assumeNotNROBecauseOfSerialization
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import kotlin.math.abs

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ClosuresTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private val origin = Point.fromLngLat(-121.496066, 38.577764)
    private val destination = Point.fromLngLat(-121.480256, 38.576795)

    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = origin.latitude()
        longitude = origin.longitude()
    }

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .routingTilesOptions(
                        RoutingTilesOptions.Builder()
                            .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                            .build(),
                    )
                    .build(),
            )
        }
    }

    @Test
    fun closuresAppearedAfterRefresh() = sdkTest {
        // no closures in the primary route
        setUpRequestHandler(R.raw.route_response_route_refresh)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh",
                // [1, 3] closure on leg#0
                readRawFileText(context, R.raw.route_response_route_refresh_annotations),
                acceptedGeometryIndex = 0,
            ),
        )

        mapboxNavigation.startTripSession()
        stayOnPosition(origin)
        mapboxNavigation.flowLocationMatcherResult().filter {
            abs(it.enhancedLocation.latitude - origin.latitude()) < 0.0005 &&
                abs(it.enhancedLocation.longitude - origin.longitude()) < 0.0005
        }
        val routes = requestRoutes(listOf(origin, destination)).take(1)
        assertTrue(routes.first().directionsRoute.legs()!!.first().closures().isNullOrEmpty())
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)

        val firstRouteProgress = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentState == RouteProgressState.TRACKING }
            .first()

        assertFalse(firstRouteProgress.hasUnexpectedUpcomingClosures())

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()

        val routeProgressAfterRefresh = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentState == RouteProgressState.TRACKING }
            .take(3).toList().last()

        assertTrue(routeProgressAfterRefresh.hasUnexpectedUpcomingClosures())

        // after closure [1, 3] start
        stayOnPosition(routes.first().directionsRoute.completeGeometryToPoints()[2])
        val routeProgressInsideClosure = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentLegProgress?.geometryIndex in 1..3 }
            .first()

        assertFalse(routeProgressInsideClosure.hasUnexpectedUpcomingClosures())
    }

    @Test
    fun hasUnavoidableClosuresThenClosureMoved() = sdkTest {
        // has closures in the alternative route
        setUpRequestHandler(R.raw.route_response_route_refresh)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh",
                // [1, 3] closure on leg#0
                readRawFileText(context, R.raw.route_response_route_refresh_no_closures),
                acceptedGeometryIndex = 0,
            ),
        )

        mapboxNavigation.startTripSession()
        stayOnPosition(origin)
        mapboxNavigation.flowLocationMatcherResult().filter {
            abs(it.enhancedLocation.latitude - origin.latitude()) < 0.0005 &&
                abs(it.enhancedLocation.longitude - origin.longitude()) < 0.0005
        }
        val routes = requestRoutes(listOf(origin, destination)).drop(1)
        assertTrue(routes.first().directionsRoute.legs()!!.first().closures()?.isNotEmpty() == true)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)

        val firstRouteProgress = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentState == RouteProgressState.TRACKING }
            .first()

        assertFalse(firstRouteProgress.hasUnexpectedUpcomingClosures())

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        val firstRefresh = mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()

        val routeProgressAfterRefresh = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentState == RouteProgressState.TRACKING }
            .take(3).toList().last()

        assertFalse(routeProgressAfterRefresh.hasUnexpectedUpcomingClosures())

        mockWebServerRule.requestHandlers[mockWebServerRule.requestHandlers.lastIndex] =
            MockDirectionsRefreshHandler(
                "route_response_route_refresh",
                // [1, 3] closure on leg#0 - closure moved
                readRawFileText(context, R.raw.route_response_route_refresh_annotations),
                acceptedGeometryIndex = 0,
            )
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH && it !== firstRefresh }
            .first()

        val routeProgressAfterRefreshWithClosure = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentState == RouteProgressState.TRACKING }
            .take(3).toList().last()

        assertTrue(routeProgressAfterRefreshWithClosure.hasUnexpectedUpcomingClosures())
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun closures_detection_works_after_deserialization() = sdkTest {
        assumeNotNROBecauseOfSerialization()
        withMapboxNavigation { navigation ->
            val mockRoute = RoutesProvider.route_alternative_with_closure(context)
            val routes = navigation.requestMockRoutes(
                mockWebServerRule,
                mockRoute,
            )

            val deserializedRouteWithClosure = withContext(Dispatchers.Default) {
                deserializeNavigationRouteFrom(
                    routes[1].serialize(),
                ).value
            }
            assertTrue(
                "Test route should have closures",
                deserializedRouteWithClosure!!.directionsRoute.legs()!![0]
                    .closures()!!.isNotEmpty(),
            )
            navigation.startTripSession()
            navigation.setNavigationRoutes(listOf(deserializedRouteWithClosure))
            navigation.moveAlongTheRouteUntilTracking(
                deserializedRouteWithClosure,
                mockLocationReplayerRule,
            )
            val routeProgress = navigation.routeProgressUpdates().first()
            assertFalse(
                "all closures should be expected",
                routeProgress.hasUnexpectedUpcomingClosures(),
            )
        }
    }

    private fun setUpRequestHandler(file: Int) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = "driving-traffic",
                jsonResponse = readRawFileText(context, file),
                expectedCoordinates = null,
                relaxedExpectedCoordinates = true,
            ),
        )
    }

    private suspend fun requestRoutes(coordinates: List<Point>): List<NavigationRoute> {
        return mapboxNavigation.requestRoutes(
            RouteOptions.builder().applyDefaultNavigationOptions()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .alternatives(false)
                .enableRefresh(true)
                .coordinatesList(coordinates)
                .baseUrl(mockWebServerRule.baseUrl)
                .build(),
        ).getSuccessfulResultOrThrowException().routes
    }

    private fun stayOnPosition(
        point: Point,
        bearing: Float = 0f,
    ) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                this.latitude = point.latitude()
                this.longitude = point.longitude()
                this.bearing = bearing
            },
            times = 120,
        )
    }
}
