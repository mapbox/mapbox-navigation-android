package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.RoutesInvalidatedParams
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.DynamicResponseModifier
import com.mapbox.navigation.instrumentation_tests.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPositionAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.refreshStates
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesInvalidatedResults
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
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
import java.util.concurrent.TimeUnit

class RefreshTtlTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var frequentRefreshOptions: RouteRefreshOptions

    private val coordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674),
    )

    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = coordinates[0].latitude()
        longitude = coordinates[0].longitude()
        bearing = 190f
    }

    @Before
    fun setUp() {
        runOnMainSync {
            frequentRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                .build()
            RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
                isAccessible = true
                set(frequentRefreshOptions, 3_000L)
            }
        }
    }

    @Test
    fun refreshTtlExpiresOnFirstRefreshForAllRoutes() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh_with_ttl),
                coordinates
            )
        )
        val routeOptions = generateRouteOptions(coordinates)
        mapboxNavigation.startTripSession()
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes

            mapboxNavigation.setNavigationRoutes(requestedRoutes)
            val routesInvalidatedResult = withTimeout(4000) {
                mapboxNavigation.routesInvalidatedResults().first()
            }
            assertEquals(requestedRoutes, routesInvalidatedResult.invalidatedRoutes)
        }
    }

    @Test
    fun refreshTtlExpiresOnDifferentRefreshesForDifferentRoutes() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh_with_different_ttls),
                coordinates
            )
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh_with_different_ttls",
                readRawFileText(context, R.raw.route_response_route_refreshed_with_different_ttls),
            )
        )
        val routeOptions = generateRouteOptions(coordinates)
        mapboxNavigation.startTripSession()
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes

            mapboxNavigation.setNavigationRoutes(requestedRoutes)

            val invalidatedResults = mutableListOf<RoutesInvalidatedParams>()
            mapboxNavigation.registerRoutesInvalidatedObserver {
                invalidatedResults.add(it)
            }
            mapboxNavigation.routesUpdates().filter {
                it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
            }.first()
            assertEquals(listOf(requestedRoutes.first()), invalidatedResults[0].invalidatedRoutes)

            withTimeout(4000) {
                mapboxNavigation.routesInvalidatedResults().first()
            }
            assertEquals(
                listOf(requestedRoutes[1].id),
                invalidatedResults[1].invalidatedRoutes.map { it.id }
            )
        }
    }

    @Test
    fun continuousAlternativeRouteIsInvalidated() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        val alternativeCoordinates = listOf(
            Point.fromLngLat(-122.2750659, 37.8052036),
            Point.fromLngLat(-122.2647245, 37.8138895)
        )
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_alternatives_with_large_ttl),
                alternativeCoordinates
            )
        )
        val originalRoutes = mapboxNavigation.requestRoutes(
            generateRouteOptions(alternativeCoordinates)
        ).getSuccessfulResultOrThrowException().routes
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.alternative_route_response_for_route_with_large_ttl),
                alternativeCoordinates,
                relaxedExpectedCoordinates = true
            )
        )
        val alternativesObserver = object : NavigationRouteAlternativesObserver {
            override fun onRouteAlternatives(
                routeProgress: RouteProgress,
                alternatives: List<NavigationRoute>,
                routerOrigin: RouterOrigin
            ) {
                mapboxNavigation.setNavigationRoutes(
                    listOf(mapboxNavigation.getNavigationRoutes().first()) + alternatives
                )
            }

            override fun onRouteAlternativesError(error: RouteAlternativesError) {
                // no-op
            }
        }

        mapboxNavigation.registerRouteAlternativesObserver(alternativesObserver)
        mapboxNavigation.startTripSession()
        stayOnPositionAndWaitForUpdate(
            mapboxNavigation,
            alternativeCoordinates[0].latitude(),
            alternativeCoordinates[0].longitude(),
            0f
        ) { }
        mapboxNavigation.setNavigationRoutes(originalRoutes)
        mockLocationReplayerRule.playRoute(originalRoutes.first().directionsRoute)
        mapboxNavigation.routesUpdates().filter {
            it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE &&
                it.navigationRoutes.any { route ->
                    route.id.startsWith("alternative_route_response_for_route_with_large_ttl")
                }
        }.first()
        val invalidatedResults = withTimeout(4000) {
            mapboxNavigation.routesInvalidatedResults().first()
        }
        assertEquals(
            listOf("alternative_route_response_for_route_with_large_ttl#0"),
            invalidatedResults.invalidatedRoutes.map { it.id }
        )
    }

    @Test
    fun refreshTtlIsUpdatedOnRefresh() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh_with_large_ttls),
                coordinates
            )
        )
        // refresh_ttl = 2
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh_with_large_ttls",
                readRawFileText(context, R.raw.route_response_route_refreshed_ttl_2),
                routeIndex = 0
            )
        )
        // refresh_ttl = 5
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh_with_large_ttls",
                readRawFileText(context, R.raw.route_response_route_refreshed_ttl_5),
                routeIndex = 1
            )
        )
        val routeOptions = generateRouteOptions(coordinates)
        mapboxNavigation.startTripSession()
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val invalidatedResults = mutableListOf<RoutesInvalidatedParams>()
            mapboxNavigation.registerRoutesInvalidatedObserver { invalidatedResults.add(it) }

            val routes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException().routes
            mapboxNavigation.setNavigationRoutes(routes)

            mapboxNavigation.routesUpdates().filter {
                it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
            }.first()
            assertEquals(0, invalidatedResults.size)

            mockWebServerRule.requestHandlers.removeLast()
            // refresh_ttl = 2
            mockWebServerRule.requestHandlers.add(
                MockDirectionsRefreshHandler(
                    "route_response_route_refresh_with_large_ttls",
                    readRawFileText(context, R.raw.route_response_route_refreshed_ttl_2),
                    routeIndex = 1
                )
            )

            val actualRoutesInvalidatedResults = mapboxNavigation
                .routesInvalidatedResults().take(2).toList()
            assertEquals(
                listOf(
                    // after second refresh
                    listOf("route_response_route_refresh_with_large_ttls#0"),
                    // after third refresh
                    listOf("route_response_route_refresh_with_large_ttls#1"),
                ),
                actualRoutesInvalidatedResults.map { it.invalidatedRoutes.map { it.id } }
            )
        }
    }

    @Test
    fun refreshTtlUpdatedOnReroute() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh_with_large_ttls),
                coordinates
            )
        )
        val routeOptions = generateRouteOptions(coordinates)
        mapboxNavigation.startTripSession()
        val invalidatedResults = mutableListOf<RoutesInvalidatedParams>()
        mapboxNavigation.registerRoutesInvalidatedObserver { invalidatedResults.add(it) }
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val routes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException().routes
            mockWebServerRule.requestHandlers.clear()
            mockWebServerRule.requestHandlers.add(
                MockDirectionsRequestHandler(
                    "driving-traffic",
                    readRawFileText(context, R.raw.route_response_with_large_ttl_reroute),
                    null,
                    relaxedExpectedCoordinates = true
                )
            )
            mockWebServerRule.requestHandlers.add(
                MockDirectionsRefreshHandler(
                    "route_response_with_large_ttl_reroute",
                    readRawFileText(context, R.raw.route_response_with_large_ttl_reroute_refresh),
                )
            )
            mapboxNavigation.setNavigationRoutes(routes)
        }
        val offRouteLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = coordinates[0].latitude()
            longitude = coordinates[0].longitude() + 0.002
        }
        stayOnPosition(offRouteLocation.latitude, offRouteLocation.longitude, 0f) {
            mapboxNavigation.routesUpdates()
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
                .first()
            mapboxNavigation.routesUpdates()
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
                .first()
            assertEquals(0, invalidatedResults.size)
            val invalidatedResult = withTimeout(4000) {
                mapboxNavigation.routesInvalidatedResults().first()
            }
            assertEquals(
                listOf("route_response_with_large_ttl_reroute#0"),
                invalidatedResult.invalidatedRoutes.map { it.id }
            )
        }
    }

    @Test
    fun noRefreshTtlInTheOriginalResponse() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh),
                coordinates
            )
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh",
                readRawFileText(context, R.raw.route_response_route_refresh_annotations)
            )
        )
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val routesInvalidatedResults = mutableListOf<RoutesInvalidatedParams>()
            mapboxNavigation.registerRoutesInvalidatedObserver { routesInvalidatedResults.add(it) }
            val routes = mapboxNavigation.requestRoutes(generateRouteOptions(coordinates))
                .getSuccessfulResultOrThrowException().routes
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutes(routes)
            mapboxNavigation.routesUpdates()
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
                .first()
            assertEquals(0, routesInvalidatedResults.size)
        }
    }

    @Test
    fun noRefreshTtlOnRefresh() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh_medium_ttl),
                coordinates
            )
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh_medium_ttl",
                readRawFileText(context, R.raw.route_response_route_refresh_annotations)
            ).also {
                it.jsonResponseModifier = DynamicResponseModifier()
            }
        )
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val routesInvalidatedResults = mutableListOf<RoutesInvalidatedParams>()
            mapboxNavigation.registerRoutesInvalidatedObserver { routesInvalidatedResults.add(it) }
            val routes = mapboxNavigation.requestRoutes(generateRouteOptions(coordinates))
                .getSuccessfulResultOrThrowException().routes
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutes(routes)
            mapboxNavigation.routesUpdates()
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
                .take(2).toList()
            assertEquals(0, routesInvalidatedResults.size)

            val routesInvalidatedResult = withTimeout(4000) {
                mapboxNavigation.routesInvalidatedResults().first()
            }
            assertEquals(
                routes.map { it.id },
                routesInvalidatedResult.invalidatedRoutes.map { it.id }
            )
        }
    }

    @Test
    fun noRefreshTtlInTheOriginalResponseButHasInRefresh() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh),
                coordinates
            )
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh",
                readRawFileText(context, R.raw.route_response_route_refreshed_ttl_2)
            ).also {
                it.jsonResponseModifier = DynamicResponseModifier()
            }
        )
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val routesInvalidatedResults = mutableListOf<RoutesInvalidatedParams>()
            mapboxNavigation.registerRoutesInvalidatedObserver { routesInvalidatedResults.add(it) }
            val routes = mapboxNavigation.requestRoutes(generateRouteOptions(coordinates))
                .getSuccessfulResultOrThrowException().routes
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutes(routes)
            mapboxNavigation.routesUpdates()
                .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
                .first()
            assertEquals(0, routesInvalidatedResults.size)

            val routesInvalidatedResult = withTimeout(4000) {
                mapboxNavigation.routesInvalidatedResults().first()
            }
            assertEquals(
                routes.map { it.id },
                routesInvalidatedResult.invalidatedRoutes.map { it.id }
            )
        }
    }

    @Test
    fun refreshFailsWithRefreshTtl0() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh_with_large_ttls),
                coordinates
            )
        )
        val refreshHandler = FailByRequestMockRequestHandler(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh_with_large_ttls",
                readRawFileText(context, R.raw.route_response_route_refreshed_ttl_2)
            )
        ).also {
            it.failResponse = true
            it.errorBody = """{"refresh_ttl": 0}"""
        }
        mockWebServerRule.requestHandlers.add(refreshHandler)
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val routesInvalidatedResults = mutableListOf<RoutesInvalidatedParams>()
            mapboxNavigation.registerRoutesInvalidatedObserver { routesInvalidatedResults.add(it) }
            val routes = mapboxNavigation.requestRoutes(generateRouteOptions(coordinates))
                .getSuccessfulResultOrThrowException().routes
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
            delay(4000) // refresh interval
            assertEquals(
                routes.map { it.id },
                routesInvalidatedResults.first().invalidatedRoutes.map { it.id }
            )
        }
    }

    @Test
    fun whenRefreshFailsWithNoTtlOldTtlIsUsed() = sdkTest {
        createMapboxNavigation(frequentRefreshOptions)
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh_medium_ttl),
                coordinates
            )
        )
        val refreshHandler = FailByRequestMockRequestHandler(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh_medium_ttl",
                readRawFileText(context, R.raw.route_response_route_refreshed_ttl_2)
            )
        ).also {
            it.failResponse = true
        }
        mockWebServerRule.requestHandlers.add(refreshHandler)
        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val routesInvalidatedResults = mutableListOf<RoutesInvalidatedParams>()
            mapboxNavigation.registerRoutesInvalidatedObserver { routesInvalidatedResults.add(it) }
            val routes = mapboxNavigation.requestRoutes(generateRouteOptions(coordinates))
                .getSuccessfulResultOrThrowException().routes
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)

            // 2 refresh attempts
            delay(6000)
            assertEquals(0, routesInvalidatedResults.size)

            val routesInvalidatedResult = withTimeout(4000) {
                mapboxNavigation.routesInvalidatedResults().first()
            }
            assertEquals(
                routes.map { it.id },
                routesInvalidatedResult.invalidatedRoutes.map { it.id }
            )
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun routeInvalidationCallbackFiresOnImmediateRouteRefresh() = sdkTest {
        createMapboxNavigation(RouteRefreshOptions.Builder().build())
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh_with_ttl),
                coordinates
            )
        )
        val refreshHandler = FailByRequestMockRequestHandler(
            MockDirectionsRefreshHandler(
                "route_response_route_refresh_with_large_ttls",
                readRawFileText(context, R.raw.route_response_route_refreshed_ttl_2)
            )
        ).also {
            it.failResponse = true
        }
        mockWebServerRule.requestHandlers.add(refreshHandler)

        stayOnPosition(coordinates[0].latitude(), coordinates[0].longitude(), 190f) {
            val routesInvalidatedResults = mutableListOf<RoutesInvalidatedParams>()
            mapboxNavigation.registerRoutesInvalidatedObserver { routesInvalidatedResults.add(it) }
            val routes = mapboxNavigation.requestRoutes(generateRouteOptions(coordinates))
                .getSuccessfulResultOrThrowException().routes
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutes(routes)

            mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
            val lastFailedResult = mapboxNavigation.refreshStates().filter {
                it.state == RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
            }.first()
            assertEquals(0, routesInvalidatedResults.size)

            delay(2000)

            mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
            mapboxNavigation.refreshStates().filter {
                it.state == RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED &&
                    it !== lastFailedResult
            }.first()

            assertEquals(
                routes.map { it.id },
                routesInvalidatedResults.first().invalidatedRoutes.map { it.id }
            )
        }
    }

    private fun generateRouteOptions(coordinates: List<Point>): RouteOptions {
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .build()
    }

    private fun createMapboxNavigation(routeRefreshOptions: RouteRefreshOptions) {
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(context)
                .accessToken(getMapboxAccessTokenFromResources(context))
                .routeRefreshOptions(routeRefreshOptions)
                .navigatorPredictionMillis(0L)
                .build()
        )
    }
}
