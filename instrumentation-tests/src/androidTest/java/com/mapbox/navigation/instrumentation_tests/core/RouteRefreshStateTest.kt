package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.annotation.IntegerRes
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
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.core.routerefresh.RouteRefreshStateResult
import com.mapbox.navigation.core.routerefresh.RouteRefreshStatesObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.DelayedResponseModifier
import com.mapbox.navigation.instrumentation_tests.utils.DynamicResponseModifier
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routesUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockRoutingTileEndpointErrorRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.NthAttemptHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshStateTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private val observer = TestObserver()

    private val coordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674)
    )

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = coordinates[0].latitude()
        longitude = coordinates[0].longitude()
    }

    @Test
    fun emptyRoutesOnDemandRefreshTest() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            createRefreshHandler(
                R.raw.route_response_route_refresh_annotations,
                "route_response_single_route_refresh"
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(1_000))
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()
        delay(2000) // refresh interval + accuracy
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun routeRefreshOnDemandForInvalidRoutes() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            createRefreshHandler(
                R.raw.route_response_route_refresh_annotations,
                "route_response_single_route_refresh"
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes(enableRefresh = false)
        mapboxNavigation.setNavigationRoutes(requestedRoutes)

        delay(7000) // refresh interval + accuracy

        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        delay(1000) // accuracy

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun invalidRoutesRefreshTest() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            createRefreshHandler(
                R.raw.route_response_route_refresh_annotations,
                "route_response_single_route_refresh"
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(1_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes(enableRefresh = false)
        mapboxNavigation.setNavigationRoutes(requestedRoutes)

        delay(2000) // refresh interval + accuracy

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun successfulRefreshTest() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            createRefreshHandler(
                R.raw.route_response_route_refresh_annotations,
                "route_response_single_route_refresh"
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(10_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutes(requestedRoutes)

        waitForRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun notStartedUntilTimeElapses() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            createRefreshHandler(
                R.raw.route_response_route_refresh_annotations,
                "route_response_single_route_refresh"
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutes(requestedRoutes)

        delay(4000)

        assertEquals(
            emptyList<String>(),
            observer.getStatesSnapshot()
        )

        waitForRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun successfulFromSecondAttemptRefreshTest() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            NthAttemptHandler(
                createRefreshHandler(
                    R.raw.route_response_route_refresh_annotations,
                    "route_response_single_route_refresh"
                ),
                1
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutes(requestedRoutes)

        waitForRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun threeFailedAttemptsThenSuccessfulRefreshTest() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh_without_traffic,
            NthAttemptHandler(
                createRefreshHandler(
                    R.raw.route_response_route_refresh_annotations_without_traffic,
                    "route_response_single_route_refresh_without_traffic"
                ),
                3
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(4_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutes(requestedRoutes)

        waitForRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_CLEARED_EXPIRED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun routeRefreshDoesNotDispatchCancelledStateOnDestroyTest() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            NthAttemptHandler(
                createRefreshHandler(
                    R.raw.route_response_route_refresh_annotations,
                    "route_response_single_route_refresh"
                ),
                2
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        delay(8000) // refresh interval + accuracy

        mapboxNavigation.onDestroy()
        delay(2000) // accuracy: to wait for potential cancelled

        assertEquals(
            listOf(RouteRefreshExtra.REFRESH_STATE_STARTED),
            observer.getStatesSnapshot()
        )
    }

    @Test(timeout = 5_000)
    fun successfulRouteRefreshOnDemandTest() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            createRefreshHandler(
                R.raw.route_response_route_refresh_annotations,
                "route_response_single_route_refresh"
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(10_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        waitForRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test(timeout = 5_000)
    fun failedRouteRefreshOnDemandTest() = sdkTest {
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            NthAttemptHandler(
                createRefreshHandler(
                    R.raw.route_response_route_refresh_annotations,
                    "route_response_single_route_refresh"
                ),
                1
            )
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(10_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        delay(2500) // execute request

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun multipleRouteRefreshesOnDemandTest() = sdkTest {
        val refreshHandler = createRefreshHandler(
            R.raw.route_response_route_refresh_annotations,
            "route_response_single_route_refresh"
        )
        refreshHandler.jsonResponseModifier = DelayedResponseModifier(
            4000,
            DynamicResponseModifier()
        )
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            refreshHandler
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(20_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        delay(2000) // start refresh request
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        delay(4000)

        waitForRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            observer.getStatesSnapshot()
        )
        assertEquals(1, refreshHandler.handledRequests.size)
    }

    @Test
    fun routeRefreshOnDemandThenPlannedTest() = sdkTest {
        val refreshHandler = createRefreshHandler(
            R.raw.route_response_route_refresh_annotations,
            "route_response_single_route_refresh"
        )
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            refreshHandler
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        delay(5000)

        waitForRefreshes(2) // immediate + planned

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
    fun routeRefreshOnDemandFailsThenPlannedTest() = sdkTest {
        val refreshHandler = createRefreshHandler(
            R.raw.route_response_route_refresh_annotations,
            "route_response_single_route_refresh"
        )
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            NthAttemptHandler(refreshHandler, 1)
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        delay(5000)

        waitForRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
            ),
            observer.getStatesSnapshot()
        )
    }

    @Test
    fun routeRefreshOnDemandBetweenPlannedAttemptsTest() = sdkTest {
        val refreshHandler = createRefreshHandler(
            R.raw.route_response_route_refresh_annotations,
            "route_response_single_route_refresh"
        )
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            NthAttemptHandler(refreshHandler, 1)
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        delay(8000) // refresh interval + accuracy

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        waitForRefreshes(2) // one from immediate and the next planned

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
    fun routeRefreshOnDemandFailsBetweenPlannedAttemptsTest() = sdkTest {
        val refreshHandler = createRefreshHandler(
            R.raw.route_response_route_refresh_annotations,
            "route_response_single_route_refresh"
        )
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        setupMockRequestHandlers(
            coordinates,
            R.raw.route_response_single_route_refresh,
            NthAttemptHandler(refreshHandler, 2)
        )

        createMapboxNavigation(createRouteRefreshOptionsWithInvalidInterval(5_000))
        mapboxNavigation.routeRefreshController.registerRouteRefreshStateObserver(observer)
        mapboxNavigation.startTripSession()
        val requestedRoutes = requestRoutes()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        delay(8000) // refresh interval + accuracy

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        waitForRefresh()

        assertEquals(
            listOf(
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                RouteRefreshExtra.REFRESH_STATE_STARTED,
                RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
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

    private fun setupMockRequestHandlers(
        coordinates: List<Point>,
        @IntegerRes routesResponse: Int,
        refreshHandler: MockRequestHandler
    ) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(MockRoutingTileEndpointErrorRequestHandler())
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, routesResponse),
                coordinates
            )
        )
        mockWebServerRule.requestHandlers.add(refreshHandler)
    }

    private fun createRefreshHandler(
        @IntegerRes refreshResponse: Int,
        testUuid: String,
    ): MockDirectionsRefreshHandler {
        return MockDirectionsRefreshHandler(
            testUuid,
            readRawFileText(activity, refreshResponse),
            null
        )
    }

    private suspend fun requestRoutes(
        coordinates: List<Point> = this.coordinates,
        enableRefresh: Boolean = true,
    ): List<NavigationRoute> =
        mapboxNavigation.requestRoutes(
            generateRouteOptions(coordinates, enableRefresh)
        )
            .getSuccessfulResultOrThrowException()
            .routes

    private fun generateRouteOptions(
        coordinates: List<Point>,
        enableRefresh: Boolean,
    ): RouteOptions {
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .enableRefresh(enableRefresh)
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .build()
    }

    private suspend fun waitForRefresh() {
        mapboxNavigation.routesUpdates().filter {
            it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
        }.first()
    }

    private suspend fun waitForRefreshes(n: Int) {
        mapboxNavigation.routesUpdates().filter {
            it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
        }.take(n).toList()
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class TestObserver : RouteRefreshStatesObserver {

    private val states = mutableListOf<RouteRefreshStateResult>()

    override fun onNewState(result: RouteRefreshStateResult) {
        states.add(result)
    }

    fun getStatesSnapshot(): List<String> = states.map { it.state }
}
