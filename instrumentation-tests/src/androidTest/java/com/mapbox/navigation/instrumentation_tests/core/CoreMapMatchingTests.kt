package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.route.deserializeNavigationRouteFrom
import com.mapbox.navigation.base.internal.route.serialize
import com.mapbox.navigation.base.options.NavigateToFinalDestination
import com.mapbox.navigation.base.options.RerouteDisabled
import com.mapbox.navigation.base.route.ResponseOriginAPI.Companion.DIRECTIONS_API
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.mapmatching.MapMatchingAPICallback
import com.mapbox.navigation.core.mapmatching.MapMatchingExtras
import com.mapbox.navigation.core.mapmatching.MapMatchingFailure
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions
import com.mapbox.navigation.core.mapmatching.MapMatchingSuccessfulResult
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.MapMatchingRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestMapMatching
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.rerouteStates
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.assertions.assertRerouteFailedTransition
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.http.MockMapMatchingRequestHandler
import com.mapbox.navigation.testing.utils.http.NotAuthorizedRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.setTestRouteRefreshInterval
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

private const val TEST_WRONG_TOKEN = "wrong-token"

private val regularOnlineRerouteFlow = listOf(
    RerouteState.Idle,
    RerouteState.FetchingRoute,
    RerouteState.RouteFetched(RouterOrigin.ONLINE),
    RerouteState.Idle,
)

@OptIn(ExperimentalMapboxNavigationAPI::class)
class CoreMapMatchingTests : BaseCoreNoCleanUpTest() {

    private val useRealServer = false

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private val origin = Point.fromLngLat(
        13.361378213031003,
        52.49813341962201,
    )

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = origin.longitude()
            latitude = origin.latitude()
        }
    }

    @Test
    fun arriveOnMapMatchedRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val options = setupTestMapMatchingRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            navigation.setNavigationRoutes(result.navigationRoutes)
            mockLocationReplayerRule.playRoute(
                result.matches.first().navigationRoute.directionsRoute,
            )
            navigation.startTripSession()
            navigation.routeProgressUpdates().first {
                it.currentState == RouteProgressState.COMPLETE
            }

            assertEquals(
                listOf(
                    Point.fromLngLat(-117.172877, 32.712021),
                    Point.fromLngLat(-117.173337, 32.71253),
                ),
                result.navigationRoutes.first().waypoints?.map { it.location() },
            )
        }
    }

    @Test
    fun arriveOnMapMatchedRouteFromOpenLR() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val options = setupOpenLrTestRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            navigation.setNavigationRoutes(result.navigationRoutes)
            mockLocationReplayerRule.playRoute(result.navigationRoutes.first().directionsRoute)
            navigation.startTripSession()
            navigation.routeProgressUpdates().first {
                it.currentState == RouteProgressState.COMPLETE
            }
        }
    }

    @Test
    fun deviateToRegularRouteAlternative() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            val (options, directionOptions) =
                setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
            val directionsAPIResult =
                navigation.requestRoutes(directionOptions).getSuccessfulResultOrThrowException()
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            val setRouteResult = navigation.setNavigationRoutesAsync(
                mapMatchingResult.navigationRoutes + directionsAPIResult.routes,
            )

            assertEquals(0, setRouteResult.value!!.ignoredAlternatives.size)
            mockLocationReplayerRule.playRoute(
                directionsAPIResult.routes.first().directionsRoute,
            )
            navigation.startTripSession()

            val routesUpdate = navigation.routesUpdates()
                .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
            assertEquals(
                directionsAPIResult.routes.single().id,
                routesUpdate.navigationRoutes.first().id,
            )
            assertEquals(
                regularOnlineRerouteFlow,
                rerouteStates,
            )
        }
    }

    @Test
    fun deviateFromRegularToMapMatchedAlternativeRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }

            val (options, directionOptions) =
                setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
            val directionsAPIResult =
                navigation.requestRoutes(directionOptions).getSuccessfulResultOrThrowException()
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            val setRouteResult = navigation.setNavigationRoutesAsync(
                directionsAPIResult.routes + mapMatchingResult.navigationRoutes,
            )

            assertEquals(0, setRouteResult.value!!.ignoredAlternatives.size)
            mockLocationReplayerRule.playRoute(
                mapMatchingResult.navigationRoutes.first().directionsRoute,
            )
            navigation.startTripSession()

            val routesUpdate = navigation.routesUpdates()
                .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
            assertEquals(
                mapMatchingResult.navigationRoutes.single().id,
                routesUpdate.navigationRoutes.first().id,
            )
            assertEquals(
                regularOnlineRerouteFlow,
                rerouteStates,
            )
        }
    }

    @Test
    fun deviateToMapMatchedAlternativeRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }

            val (primaryMapMatched, _, mapMatchedAlternativeOptions) =
                setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
            val mapMatchingPrimary =
                navigation.requestMapMatching(primaryMapMatched).getSuccessfulOrThrowException()
            val mapMatchingAlternative =
                navigation.requestMapMatching(mapMatchedAlternativeOptions)
                    .getSuccessfulOrThrowException()
            val setRouteResult = navigation.setNavigationRoutesAsync(
                mapMatchingPrimary.navigationRoutes + mapMatchingAlternative.navigationRoutes,
            )

            assertEquals(0, setRouteResult.value!!.ignoredAlternatives.size)
            mockLocationReplayerRule.playRoute(
                mapMatchingAlternative.navigationRoutes.first().directionsRoute,
            )
            navigation.startTripSession()

            val routesUpdate = navigation.routesUpdates()
                .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
            assertEquals(
                mapMatchingAlternative.navigationRoutes.single().id,
                routesUpdate.navigationRoutes.first().id,
            )
            assertEquals(
                regularOnlineRerouteFlow,
                rerouteStates,
            )
        }
    }

    @Test
    fun refreshMixedRoutes() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            useRealTiles = true,
        ) { navigation ->
            val routeRefreshStates = mutableListOf<String>()
            navigation.routeRefreshController.registerRouteRefreshStateObserver {
                routeRefreshStates.add(it.state)
            }

            val (options, directionOptions) =
                setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
            val directionsAPIResult = navigation.requestRoutes(directionOptions)
                .getSuccessfulResultOrThrowException()
            val mapMatchingResult = navigation.requestMapMatching(options)
                .getSuccessfulOrThrowException()
            val setRouteResult = navigation.setNavigationRoutesAsync(
                directionsAPIResult.routes + mapMatchingResult.navigationRoutes,
            )
            assertEquals(0, setRouteResult.value!!.ignoredAlternatives.size)

            val origin = directionOptions.coordinatesList().first()
            stayOnPosition(
                latitude = origin.latitude(),
                longitude = origin.longitude(),
                bearing = 131.70f,
            ) {
                navigation.startTripSession()
                navigation.routeProgressUpdates().first()

                navigation.routeRefreshController.requestImmediateRouteRefresh()
                navigation.routesUpdates()
                    .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
                assertEquals(
                    listOf(
                        RouteRefreshExtra.REFRESH_STATE_STARTED,
                        RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    ),
                    routeRefreshStates,
                )
            }
        }
    }

    @Test
    fun offRouteOnMapMatchedRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            val options = setupTestMapMatchingRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            navigation.setNavigationRoutes(result.navigationRoutes)
            stayOnPosition(
                latitude = 32.712012,
                longitude = -117.172928,
                bearing = 178.0f,
                frequencyHz = 5,
            ) {
                navigation.startTripSession()
                navigation.getRerouteController()?.rerouteStates()?.first {
                    it !is RerouteState.Idle
                }
                navigation.getRerouteController()?.rerouteStates()?.first {
                    it is RerouteState.Idle
                }
                assertRerouteFailedTransition(rerouteStates)
            }
        }
    }

    @Test
    fun offRouteOnDeserializedMapMatchedRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            val options = setupTestMapMatchingRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()

            val deserializationResult = withContext(Dispatchers.Default) {
                deserializeNavigationRouteFrom(
                    result.navigationRoutes.first().serialize(),
                )
            }
            assertNull(
                deserializationResult.error,
            )
            val deserializedRoute = deserializationResult.value!!

            navigation.setNavigationRoutes(listOf(deserializedRoute))
            stayOnPosition(
                latitude = 32.712012,
                longitude = -117.172928,
                bearing = 178.0f,
                frequencyHz = 5,
            ) {
                navigation.startTripSession()
                navigation.routeProgressUpdates()
                    .filter { it.currentState == RouteProgressState.OFF_ROUTE }
                    .drop(3)
                    .first()
                assertRerouteFailedTransition(rerouteStates)
            }
        }
    }

    @Test
    fun offRouteOnCustomMapMatchedRouteFallbackToDirectionsApiAndFinalDestination() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            rerouteStrategyForMapMatchedRoutes = NavigateToFinalDestination,
        ) { navigation ->
            val geometryToDeviate = setupMockRouteAfterDeviation()

            val rerouteStates = mutableListOf<RerouteState>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }

            val options = setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
                .primaryMapMatchedRouteOptions
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            // use MM route as the primary
            navigation.setNavigationRoutes(mapMatchingResult.navigationRoutes)
            // replay Directions route just to trigger off route event
            mockLocationReplayerRule.playGeometry(geometryToDeviate)
            navigation.startTripSession()

            val newRoute = navigation.routesUpdates()
                .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
                .navigationRoutes
                .first()

            // DirectionsAPI is used as a fallback for a MM route
            assertEquals(DIRECTIONS_API, newRoute.responseOriginAPI)
            assertEquals(2, newRoute.waypoints?.size)

            // Directions API could slightly move destination due to snapping
            val expectedDestination = mapMatchingResult.navigationRoutes
                .first()
                .waypoints!!
                .last()
                .location()
            val actualDestination = newRoute.waypoints!!.last().location()
            val distanceBetweenExpectedAndActualDestinationKm = TurfMeasurement.distance(
                expectedDestination,
                actualDestination,
            )
            assertTrue(
                "actual destination($actualDestination) is more than 10 meters " +
                    "away from expected($expectedDestination)",
                distanceBetweenExpectedAndActualDestinationKm < 0.01,
            )

            assertEquals(regularOnlineRerouteFlow, rerouteStates)
        }
    }

    @Test
    fun offRouteOnMapMatchedRouteFallbackToDirectionsApiAndFinalDestinationMultiLeg() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            rerouteStrategyForMapMatchedRoutes = NavigateToFinalDestination,
        ) { navigation ->
            val geometryToDeviate = setupMockRouteAfterDeviation()

            val rerouteStates = mutableListOf<RerouteState>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }

            val options = setupTwoLegsMapMatchingRoute()
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            navigation.setNavigationRoutes(mapMatchingResult.navigationRoutes)
            val initialPrimaryRoute = mapMatchingResult.navigationRoutes.first()
            assertEquals(
                3,
                initialPrimaryRoute.waypoints?.size,
            )

            // geometry that deviates from the route and goes to the destination
            mockLocationReplayerRule.playGeometry(geometryToDeviate)
            navigation.startTripSession()

            val newRoute = navigation.routesUpdates()
                .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
                .navigationRoutes
                .first()

            assertEquals(
                "fallback routes should be provided by Directions API",
                DIRECTIONS_API,
                newRoute.responseOriginAPI,
            )
            assertEquals(
                "unvisited waypoints should be dropped",
                2,
                newRoute.waypoints?.size,
            )

            // Directions API could slightly move destination due to snapping
            val expectedDestination = mapMatchingResult.navigationRoutes
                .first()
                .waypoints!!
                .last()
                .location()
            val actualDestination = newRoute.waypoints!!.last().location()
            val distanceBetweenExpectedAndActualDestinationKm = TurfMeasurement.distance(
                expectedDestination,
                actualDestination,
            )
            assertTrue(
                "actual destination($actualDestination) is more than 10 meters " +
                    "away from expected($expectedDestination)",
                distanceBetweenExpectedAndActualDestinationKm < 0.01,
            )

            assertEquals(regularOnlineRerouteFlow, rerouteStates)
        }
    }

    @Test
    fun offRouteOnCustomMapMatchedRouteFailsOnRerouteDisabledStrategy() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            rerouteStrategyForMapMatchedRoutes = RerouteDisabled,
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }

            val waitForReroute = CompletableDeferred<Unit>()
            navigation.registerOffRouteObserver { offRoute ->
                if (offRoute) {
                    waitForReroute.complete(Unit)
                }
            }

            val (options, directionOptions) =
                setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
            val directionsAPIResult =
                navigation.requestRoutes(directionOptions).getSuccessfulResultOrThrowException()
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            // use MM route as the primary
            navigation.setNavigationRoutes(mapMatchingResult.navigationRoutes)
            // replay Directions route just to trigger off route event
            mockLocationReplayerRule.playRoute(directionsAPIResult.routes.first().directionsRoute)
            navigation.startTripSession()

            waitForReroute.await()

            val message = "According to rerouteStrategyForMapMatchedRoutes new " +
                "routes calculation for routes from Mapbox Map Matching API is disabled."
            val expectedStates = listOf(
                RerouteState.Idle,
                RerouteState.FetchingRoute,
                RerouteState.Failed(message),
                RerouteState.Idle,
            )

            assertEquals(expectedStates, rerouteStates)
        }
    }

    @Test
    fun refreshOfMapMatchedRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            routeRefreshOptions = RouteRefreshOptions
                .Builder()
                .build().apply {
                    setTestRouteRefreshInterval(1000)
                },
        ) { navigation ->
            val routeRefreshStates = mutableListOf<String>()
            navigation.routeRefreshController.registerRouteRefreshStateObserver {
                routeRefreshStates.add(it.state)
            }
            val options = setupTestMapMatchingRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            navigation.setNavigationRoutes(result.navigationRoutes)
            stayOnPosition(
                latitude = 32.71204,
                longitude = -117.17282,
                bearing = 330.0f,
                frequencyHz = 5,
            ) {
                navigation.startTripSession()
                navigation.routeProgressUpdates().first()
                delay(2000)
                assertEquals(
                    "map matched routes can't be refreshed",
                    listOf(
                        RouteRefreshExtra.REFRESH_STATE_STARTED,
                        RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    ),
                    routeRefreshStates,
                )
                delay(1000)
                assertEquals(
                    "No retry of refreshing happens for map matched routes",
                    listOf(
                        RouteRefreshExtra.REFRESH_STATE_STARTED,
                        RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    ),
                    routeRefreshStates,
                )
                routeRefreshStates.clear()
                navigation.routeRefreshController.requestImmediateRouteRefresh()
                assertEquals(
                    "Refresh by request should fail for map matched route",
                    listOf(
                        RouteRefreshExtra.REFRESH_STATE_STARTED,
                        RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                    ),
                    routeRefreshStates,
                )
            }
        }
    }

    @Test
    fun no_segments_found_error() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            mockWebServerRule.requestHandlers.add(
                MockRequestHandler { request ->
                    if (request.path?.contains("matching") == true) {
                        MockResponse()
                            .setBody(
                                readRawFileText(context, R.raw.map_matching_no_segments_found),
                            )
                            .setResponseCode(200)
                    } else {
                        null
                    }
                },
            )
            val options = MapMatchingOptions.Builder()
                .coordinates("-71.443158%2C39.613564%3B-71.448504%2C39.596188")
                .setupBaseUrl()
                .build()
            val result = navigation.requestMapMatching(options) as MapMatchingRequestResult.Failure
        }
    }

    @Test
    fun unauthorised() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            MapboxOptions.accessToken = TEST_WRONG_TOKEN
            val options = setupTestMapMatchingRoute()
            val result = navigation.requestMapMatching(options) as MapMatchingRequestResult.Failure
        }
    }

    @Test
    fun noInternet() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val options = setupTestMapMatchingRoute()
            withoutInternet {
                val result = navigation.requestMapMatching(options)
                    as MapMatchingRequestResult.Failure
            }
        }
    }

    @Test
    fun requestCancellation() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val options = setupTestMapMatchingRoute()
            val cancelled = CompletableDeferred<Unit>()
            val requestId = navigation.requestMapMatching(
                options,
                object : MapMatchingAPICallback {
                    override fun success(result: MapMatchingSuccessfulResult) {
                        fail("success callback shouldn't be called")
                    }

                    override fun failure(failure: MapMatchingFailure) {
                        fail("failure callback shouldn't be called")
                    }

                    override fun onCancel() {
                        cancelled.complete(Unit)
                    }
                },
            )
            navigation.cancelMapMatchingRequest(requestId)
            cancelled.await()
        }
    }

    @Test
    fun destroyNavigation() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            val options = setupTestMapMatchingRoute()
            val cancelled = CompletableDeferred<Unit>()
            navigation.requestMapMatching(
                options,
                object : MapMatchingAPICallback {
                    override fun success(result: MapMatchingSuccessfulResult) {
                        fail("success callback shouldn't be called")
                    }

                    override fun failure(failure: MapMatchingFailure) {
                        fail("failure callback shouldn't be called")
                    }

                    override fun onCancel() {
                        cancelled.complete(Unit)
                    }
                },
            )
            MapboxNavigationProvider.destroy()
            cancelled.await()
        }
    }

    private fun setupTwoLegsMapMatchingRoute(): MapMatchingOptions {
        val testMapMatchingCoordinates = "-117.13639789301004,32.70110487161075;" +
            "-117.13634610066397,32.70144108483845;" +
            "-117.13665685474118,32.70163409556284;" +
            "-117.13698980553816,32.70183955813353;" +
            "-117.13742890677699,32.7021702874289;" +
            "-117.13765207472014,32.702316146465435;" +
            "-117.13768890826432,32.702562283049474" +
            ";-117.13748740711172,32.70278107055441;" +
            "-117.13727290588494,32.702779247327385" +
            ";-117.13695223738412,32.702551343660545;" +
            "-117.13678540309661,32.70243647999132" +
            ";-117.13661800229082,32.702321470299225;" +
            "-117.1363947543083,32.70217044490502"
        val primaryMapMatchingRouteResponse =
            R.raw.san_diego_map_matching_two_legs
        mockWebServerRule.requestHandlers.add(
            MockMapMatchingRequestHandler(
                testMapMatchingCoordinates,
            ) {
                readRawFileText(
                    context,
                    primaryMapMatchingRouteResponse,
                )
            },
        )

        return MapMatchingOptions.Builder()
            .coordinates(testMapMatchingCoordinates)
            .setupBaseUrl()
            .waypoints(listOf(0, 8, 12))
            .tidy(true)
            .build()
    }

    // route which is supposed to be generated when user deviates from
    // R.raw.san_diego_map_matching_alternative_to_direction_route
    private fun setupMockRouteAfterDeviation(): String {
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                lazyJsonResponse = {
                    readRawFileText(
                        context,
                        R.raw.san_diego_direction_route_reroute_from_mm,
                    )
                },
                expectedCoordinates = listOf(
                    Point.fromLngLat(-117.1360077, 32.7014471),
                    Point.fromLngLat(-117.1363948, 32.7021704),
                ),
            ),
        )
        return "qh|j}@t}ll~EaLuKoYuXmQjYS\\iHfL"
    }

    private fun setupTestMapMatchingRoute(): MapMatchingOptions {
        val testCoordinates = "-117.17282,32.71204;-117.17288,32.71225;-117.17293,32.71244" +
            ";-117.17292,32.71256;-117.17298,32.712603;-117.17314,32.71259;-117.17334,32.71254"
        mockWebServerRule.requestHandlers.add(
            NotAuthorizedRequestHandler(TEST_WRONG_TOKEN),
        )
        mockWebServerRule.requestHandlers.add(
            MockMapMatchingRequestHandler(
                testCoordinates,
            ) {
                readRawFileText(context, R.raw.map_matching_example)
            },
        )
        val mapMatchingOptions = MapMatchingOptions.Builder()
            .coordinates(
                testCoordinates,
            )
            .annotations(
                listOf(
                    MapMatchingExtras.ANNOTATION_DURATION,
                    MapMatchingExtras.ANNOTATION_CONGESTION,
                ),
            )
            .voiceInstructions(true)
            .bannerInstructions(true)
            .language("en-US")
            .waypointNames(
                listOf(
                    "origin",
                    "destination",
                ),
            )
            .radiuses(MutableList(7) { 20.0 })
            .roundaboutExits(true)
            .waypoints(listOf(0, 6))
            .setupBaseUrl()
            .build()
        return mapMatchingOptions
    }

    private fun setupOpenLrTestRoute(): MapMatchingOptions {
        val testCoordinates = "CwOiYCUMoBNWAv9P/+MSBg=="
        mockWebServerRule.requestHandlers.add(
            MockMapMatchingRequestHandler(
                testCoordinates,
            ) {
                readRawFileText(context, R.raw.map_matching_example)
            },
        )
        val options = MapMatchingOptions.Builder()
            .coordinates(
                testCoordinates,
            )
            .tidy(true)
            .annotations(
                listOf(
                    MapMatchingExtras.ANNOTATION_DURATION,
                    MapMatchingExtras.ANNOTATION_CONGESTION,
                ),
            )
            .voiceInstructions(true)
            .bannerInstructions(true)
            .roundaboutExits(true)
            .setupBaseUrl()
            .build()
        return options
    }

    private data class MapMatchedRouteWithAlternatives(
        val primaryMapMatchedRouteOptions: MapMatchingOptions,
        val directionsRouteAlternative: RouteOptions,
        val mapMatchedRouteAlternative: MapMatchingOptions,
        val primaryMapMatchedRouteResponseResource: Int,
    )

    private fun setupAlternativeRoutesFromMapMatchingAndDirectionsAPI():
        MapMatchedRouteWithAlternatives {
        val testMapMatchingCoordinates = "-117.13639789301004,32.70110487161075;" +
            "-117.13634610066397,32.70144108483845;" +
            "-117.13665685474118,32.70163409556284;" +
            "-117.13698980553816,32.70183955813353;" +
            "-117.13742890677699,32.7021702874289;" +
            "-117.13765207472014,32.702316146465435;" +
            "-117.13768890826432,32.702562283049474" +
            ";-117.13748740711172,32.70278107055441;" +
            "-117.13727290588494,32.702779247327385" +
            ";-117.13695223738412,32.702551343660545;" +
            "-117.13678540309661,32.70243647999132" +
            ";-117.13661800229082,32.702321470299225;" +
            "-117.1363947543083,32.70217044490502"
        val primaryMapMatchingRouteResponse =
            R.raw.san_diego_map_matching_alternative_to_direction_route
        mockWebServerRule.requestHandlers.add(
            MockMapMatchingRequestHandler(
                testMapMatchingCoordinates,
            ) {
                readRawFileText(
                    context,
                    primaryMapMatchingRouteResponse,
                )
            },
        )
        val mapMatchingOptions = MapMatchingOptions.Builder()
            .coordinates(testMapMatchingCoordinates)
            .setupBaseUrl()
            .waypoints(listOf(0, 12))
            .tidy(true)
            .build()

        val testDirectionsAPICoordinates = listOf(
            Point.fromLngLat(-117.13639789301004, 32.70110487161075),
            Point.fromLngLat(-117.1363947543083, 32.70217044490502),
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                readRawFileText(
                    context,
                    R.raw.san_diego_direction_route_alternative_to_map_matching,
                ),
                expectedCoordinates = testDirectionsAPICoordinates,
            ),
        )
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRefreshHandler(
                testUuid = "Jyh_Hr_wpsPTo35ud7wBJytEKbVw6CrIKL0IjFQhO5P8Ntn9FBvPQg==_eu-west-1",
                readRawFileText(
                    context,
                    R.raw.san_diego_direction_route_alternative_to_map_matching_refresh,
                ),
            ),
        )
        val directionOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .setupBaseUrl()
            .coordinatesList(testDirectionsAPICoordinates)
            .build()

        val mapMatchedAlternativeCoordinates = "-117.13639789301004,32.70110487161075;" +
            "-117.13605169602593,32.70139332744387;" +
            "-117.13589294349624,32.70155343541197;" +
            "-117.13577846968222,32.701671994436566;" +
            "-117.13589294349624,32.70182315696512;" +
            "-117.13608138500544,32.701952089507785;" +
            "-117.1363947543083,32.70217044490502"
        mockWebServerRule.requestHandlers.add(
            MockMapMatchingRequestHandler(
                mapMatchedAlternativeCoordinates,
            ) {
                readRawFileText(
                    context,
                    R.raw.san_diego_map_matching_alternative_to_map_matching,
                )
            },
        )
        val mapMatchedAlternativeOptions = MapMatchingOptions.Builder()
            .coordinates(mapMatchedAlternativeCoordinates)
            .setupBaseUrl()
            .waypoints(listOf(0, 6))
            .build()

        val expectedCoordinatesAfterDeviationFromPrimaryMapMatchedRoute = listOf(
            Point.fromLngLat(-117.1360077, 32.7014471),
            Point.fromLngLat(-117.1363461, 32.7014411),
            Point.fromLngLat(-117.1366569, 32.7016341),
            Point.fromLngLat(-117.1369898, 32.7018396),
            Point.fromLngLat(-117.1374289, 32.7021703),
            Point.fromLngLat(-117.1376521, 32.7023161),
            Point.fromLngLat(-117.1376889, 32.7025623),
            Point.fromLngLat(-117.1374874, 32.7027811),
            Point.fromLngLat(-117.1372729, 32.7027792),
            Point.fromLngLat(-117.1369522, 32.7025513),
            Point.fromLngLat(-117.1367854, 32.7024365),
            Point.fromLngLat(-117.136618, 32.7023215),
            Point.fromLngLat(-117.1363948, 32.7021704),
        )

        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                readRawFileText(
                    context,
                    R.raw.san_diego_direction_reroute_after_deviation_fom_map_matched,
                ),
                expectedCoordinates = expectedCoordinatesAfterDeviationFromPrimaryMapMatchedRoute,
            ),
        )

        return MapMatchedRouteWithAlternatives(
            mapMatchingOptions,
            directionOptions,
            mapMatchedAlternativeOptions,
            primaryMapMatchingRouteResponse,
        )
    }

    private fun RouteOptions.Builder.setupBaseUrl(): RouteOptions.Builder {
        return if (!useRealServer) {
            baseUrl(mockWebServerRule.baseUrl)
        } else {
            this
        }
    }

    private fun MapMatchingOptions.Builder.setupBaseUrl(): MapMatchingOptions.Builder {
        return if (!useRealServer) {
            baseUrl(mockWebServerRule.baseUrl)
        } else {
            this
        }
    }
}
