package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.DelayedResponseModifier
import com.mapbox.navigation.instrumentation_tests.utils.DynamicResponseModifier
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockRoutingTileEndpointErrorRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.NthAttemptHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForAlternativesUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshOnDemandTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var baseRefreshHandler: MockDirectionsRefreshHandler
    private lateinit var mapboxNavigation: MapboxNavigation
    private val twoCoordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674)
    )

    @Before
    fun setUp() {
        baseRefreshHandler = MockDirectionsRefreshHandler(
            "route_response_single_route_refresh",
            readRawFileText(activity, R.raw.route_response_route_refresh_annotations),
        )
    }

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = twoCoordinates[0].latitude()
        longitude = twoCoordinates[0].longitude()
        bearing = 190f
    }

    @Test
    fun immediate_route_refresh_before_planned() = sdkTest {
        val observer = TestObserver()
        val routeRefreshes = mutableListOf<RoutesUpdatedResult>()
        setupMockRequestHandlers(baseRefreshHandler)
        baseRefreshHandler.jsonResponseModifier = DynamicResponseModifier()
        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5000))
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.registerRoutesObserver {
            if (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH) {
                routeRefreshes.add(it)
            }
        }
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        delay(2500)

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        val refreshedRoutes = mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()
        assertEquals(1, routeRefreshes.size)
        assertEquals(
            224.2239,
            requestedRoutes[0].getSumOfDurationAnnotationsFromLeg(0),
            0.0001
        )
        assertEquals(
            258.767,
            refreshedRoutes.navigationRoutes[0].getSumOfDurationAnnotationsFromLeg(0),
            0.0001
        )

        // no route refresh 4 seconds after refresh on demand
        delay(4000)
        assertEquals(1, routeRefreshes.size)

        delay(1000)
        // has new refresh 5 seconds after refresh on demand
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .take(2)
            .toList()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun route_refresh_on_demand_between_planned_attempts() = sdkTest {
        val observer = TestObserver()
        baseRefreshHandler.jsonResponseModifier = DynamicResponseModifier()
        setupMockRequestHandlers(
            NthAttemptHandler(baseRefreshHandler, 1)
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        delay(8000) // refresh interval + accuracy

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        // one from immediate and the next planned
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .take(2)
            .toList()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun route_refresh_on_demand_after_alternatives_change_between_attempts() = sdkTest {
        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_route_refresh),
                twoCoordinates
            )
        )
        val refreshHandler = MockDirectionsRefreshHandler(
            "route_response_route_refresh",
            readRawFileText(activity, R.raw.route_response_route_refresh_annotations),
        )
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        mockWebServerRule.requestHandlers.add(NthAttemptHandler(refreshHandler, 1))
        val observer = TestObserver()

        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        stayOnInitialPosition()
        mapboxNavigation.startTripSession()
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        val initialRoutes = requestedRoutes.take(1)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(initialRoutes)

        delay(7000) // in-between attempts

        // add alternatives
        mapboxNavigation.setNavigationRoutesAndWaitForAlternativesUpdate(requestedRoutes)

        val refreshes = mutableListOf<RoutesUpdatedResult>()
        mapboxNavigation.registerRoutesObserver {
            if (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH &&
                it.navigationRoutes.map { it.id } == requestedRoutes.map { it.id }) {
                refreshes.add(it)
            }
        }
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        delay(4000)
        assertEquals(1, refreshes.size) // 1 refresh from refresh on demand

        withTimeout(2500) { // 5s interval, 1.5s accuracy
            while (refreshes.size < 2) {
                delay(100)
            }
        }
        assertEquals(2, refreshes.size)

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun route_refresh_on_demand_after_reroute() = sdkTest {
        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        setupMockRequestHandlers(baseRefreshHandler)

        val observer = TestObserver()

        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        val offRouteLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = twoCoordinates[0].latitude() + 0.002
            longitude = twoCoordinates[0].longitude() + 0.001
        }

        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_reroute_for_refresh),
                listOf(
                    Point.fromLngLat(offRouteLocation.longitude, offRouteLocation.latitude),
                    twoCoordinates[1]
                )
            )
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_reroute_for_refresh",
                readRawFileText(activity, R.raw.route_response_reroute_for_refresh_refreshed)
            )
        )

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        delay(1000)
        // off-route
        stayOnPosition(offRouteLocation.latitude, offRouteLocation.longitude, 120f) {
            val reroute = mapboxNavigation.routesUpdates().first {
                it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
            }

            mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

            val refresh = withTimeout(3000) {
                mapboxNavigation.routesUpdates().first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            }
            assertEquals(reroute.navigationRoutes.map { it.id }, refresh.navigationRoutes.map { it.id })

            assertEquals(
                listOf(
                    RouteRefreshExtra.REFRESH_STATE_STARTED,
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                ),
                observer.getStatesSnapshot()
            )
        }
    }

    @Test
    fun routes_are_updated_while_refresh_on_demand_request_is_in_progress() = sdkTest {
        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(100_000))
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_route_refresh),
                twoCoordinates
            )
        )
        val refreshHandler = MockDirectionsRefreshHandler(
            "route_response_route_refresh",
            readRawFileText(activity, R.raw.route_response_route_refresh_annotations),
        )
        refreshHandler.jsonResponseModifier = DelayedResponseModifier(3000, DynamicResponseModifier())
        mockWebServerRule.requestHandlers.add(refreshHandler)
        val observer = TestObserver()

        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        val initialRoutes = requestedRoutes.take(1)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(initialRoutes)

        val refreshes = mutableListOf<RoutesUpdatedResult>()
        mapboxNavigation.registerRoutesObserver {
            if (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH) {
                refreshes.add(it)
            }
        }

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        delay(1000)
        mapboxNavigation.setNavigationRoutesAndWaitForAlternativesUpdate(requestedRoutes)

        delay(3000)
        assertEquals(0, refreshes.size)

        refreshHandler.jsonResponseModifier = { it }
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        val refreshedRoutes = withTimeout(2000) {//
            mapboxNavigation.routesUpdates().first {
                it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
            }.navigationRoutes
        }
        assertEquals(requestedRoutes.map { it.id }, refreshedRoutes.map { it.id })

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            observer.getStatesSnapshot()
        )
    }

    private fun createMapboxNavigation(routeRefreshOptions: RouteRefreshOptions) {
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

    private fun stayOnInitialPosition() {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = twoCoordinates[0].latitude()
                longitude = twoCoordinates[0].longitude()
                bearing = 190f
            },
            times = 120
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

    private fun setupMockRequestHandlers(
        refreshHandler: MockRequestHandler,
    ) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_single_route_refresh),
                twoCoordinates
            )
        )
        mockWebServerRule.requestHandlers.add(refreshHandler)
        mockWebServerRule.requestHandlers.add(MockRoutingTileEndpointErrorRequestHandler())
    }

    private fun NavigationRoute.getSumOfDurationAnnotationsFromLeg(legIndex: Int): Double =
        directionsRoute.legs()?.get(legIndex)
            ?.annotation()
            ?.duration()
            ?.sum()!!

    private fun createRouteRefreshOptionsWithInvalidInterval(
        intervalMillis: Long
    ): RouteRefreshOptions {
        val routeRefreshOptions = RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
            .build()
        RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
            isAccessible = true
            set(routeRefreshOptions, intervalMillis)
        }
        return routeRefreshOptions
    }
}
