package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.annotation.IntegerRes
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.DynamicResponseModifier
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routesUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockRoutingTileEndpointErrorRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.TimeUnit

class RouteRefreshOnDemandTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var failedRefreshHandlerWrapper: FailByRequestMockRequestHandler
    private lateinit var refreshHandler: MockDirectionsRefreshHandler
    private lateinit var mapboxNavigation: MapboxNavigation
    private val twoCoordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674)
    )

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = twoCoordinates[0].latitude()
        longitude = twoCoordinates[0].longitude()
        bearing = 190f
    }

    @Before
    fun setup() {
        setupMockRequestHandlers(
            twoCoordinates,
            R.raw.route_response_route_refresh,
            R.raw.route_response_route_refresh_annotations,
            "route_response_route_refresh"
        )
    }

    @After
    fun tearDown() {
        failedRefreshHandlerWrapper.failResponse = false
    }

    @Test(timeout = 10_000)
    fun route_refresh_on_demand_executes_before_refresh_interval() = sdkTest {
        val routeRefreshOptions = RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.MINUTES.toMillis(1))
            .build()
        createMapboxNavigation(routeRefreshOptions)
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        mapboxNavigation.refreshRoutesImmediately()
        val refreshedRoutes = mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()

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
    }

    @Test
    fun route_refresh_on_demand_invalidates_planned_timer() = sdkTest {
        val routeRefreshes = mutableListOf<RoutesUpdatedResult>()
        val routeRefreshOptions = RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
            .build()
        RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
            isAccessible = true
            set(routeRefreshOptions, 5_000L)
        }
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        createMapboxNavigation(routeRefreshOptions)
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.registerRoutesObserver {
            if (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH) {
                routeRefreshes.add(it)
            }
        }
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        delay(2500)

        mapboxNavigation.refreshRoutesImmediately()
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()
        assertEquals(1, routeRefreshes.size)

        // no route refresh 4 seconds after refresh on demand
        delay(4000)
        assertEquals(1, routeRefreshes.size)

        delay(1000)
        // has new refresh 5 seconds after refresh on demand
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .take(2)
            .toList()
    }

    @Test
    fun failed_route_refresh_on_demand_does_not_notify_refresh_observer_before_timeout() = sdkTest {
        val refreshes = mutableListOf<RoutesUpdatedResult>()
        val routeRefreshOptions = RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.MINUTES.toMillis(1))
            .build()
        failedRefreshHandlerWrapper.failResponse = true
        createMapboxNavigation(routeRefreshOptions)
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.startTripSession()
        mapboxNavigation.registerRoutesObserver {
            if (it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH) {
                refreshes.add(it)
            }
        }
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        mapboxNavigation.refreshRoutesImmediately()
        delay(2000)
        assertEquals(0, refreshes.size)
    }

    @Test
    fun failed_route_refresh_on_demand_notifies_refresh_observer_after_timeout() = sdkTest {
        val routeRefreshOptions = createRouteRefreshOptionsWithInvalidInterval(3000)
        failedRefreshHandlerWrapper.failResponse = true
        createMapboxNavigation(routeRefreshOptions)
        val routeOptions = generateRouteOptions(twoCoordinates)
        val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        delay(7000) // 2 failed planned attempts + accuracy
        mapboxNavigation.refreshRoutesImmediately() // fail and postpone next planned attempt
        delay(2000)
        mapboxNavigation.refreshRoutesImmediately() // dispatch new routes with REFRESH reason

        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()
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
        coordinates: List<Point>,
        @IntegerRes routesResponse: Int,
        @IntegerRes refreshResponse: Int,
        responseTestUuid: String,
        acceptedGeometryIndex: Int? = null,
    ) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, routesResponse),
                coordinates
            )
        )
        refreshHandler = MockDirectionsRefreshHandler(
            responseTestUuid,
            readRawFileText(activity, refreshResponse),
            acceptedGeometryIndex
        )
        failedRefreshHandlerWrapper = FailByRequestMockRequestHandler(refreshHandler)
        mockWebServerRule.requestHandlers.add(failedRefreshHandlerWrapper)
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
