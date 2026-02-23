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
import com.mapbox.navigation.base.route.NavigationRoute
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
import com.mapbox.navigation.core.reroute.RerouteStateV2
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.MapMatchingRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestMapMatching
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.rerouteStates
import com.mapbox.navigation.testing.ui.utils.coroutines.rerouteStatesV2
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.assertions.assertRerouteFailedTransition
import com.mapbox.navigation.testing.utils.assertions.assertRerouteFailedTransitionV2
import com.mapbox.navigation.testing.utils.assertions.assertSuccessfulRouteAppliedRerouteStateTransition
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.http.MockMapMatchingRequestHandler
import com.mapbox.navigation.testing.utils.http.NotAuthorizedRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.nativeRerouteControllerNoRetryConfig
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.setTestRouteRefreshInterval
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val TEST_WRONG_TOKEN = "wrong-token"

private val regularOnlineRerouteFlow = listOf(
    RerouteState.Idle,
    RerouteState.FetchingRoute,
    RerouteState.RouteFetched(RouterOrigin.ONLINE),
    RerouteState.Idle,
)

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(Parameterized::class)
class CoreMapMatchingTests(
    private val runOptions: RerouteTestRunOptions,
) : BaseCoreNoCleanUpTest() {

    data class RerouteTestRunOptions(
        val nativeReroute: Boolean,
    ) {

        override fun toString(): String {
            return if (nativeReroute) {
                "native reroute"
            } else {
                "platform reroute"
            }
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            RerouteTestRunOptions(nativeReroute = false),
            RerouteTestRunOptions(nativeReroute = true),
        )
    }

    private val useRealServer = false

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private fun getTestCustomConfig(): String = if (runOptions.nativeReroute) {
        nativeRerouteControllerNoRetryConfig
    } else {
        ""
    }

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

    /**
     * Test: Basic navigation completion on a map-matched route
     *
     * Use Case: Validates that map-matched routes can be successfully used for turn-by-turn navigation
     *
     * Flow:
     * 1. Request map matching with test coordinates (7 GPS points)
     * 2. Set the first matched route as the active navigation route
     * 3. Replay the route to simulate vehicle movement
     * 4. Monitor route progress until COMPLETE state is reached
     *
     * Validates:
     * - Map-matched routes work for turn-by-turn navigation
     * - Route completion is properly detected
     * - Waypoints are correctly extracted (2 waypoints: start and end)
     *
     * Example: Replay a historical delivery route with GPS trace from San Diego
     */
    @Test
    fun arriveOnMapMatchedRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
        ) { navigation ->
            val options = setupTestMapMatchingRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            // Use only the first match as the route
            navigation.setNavigationRoutes(listOf(result.matches.first().navigationRoute))
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
                result.matches.first().navigationRoute.waypoints?.map { it.location() },
            )
        }
    }

    /**
     * Test: Navigation using OpenLR (Open Location Reference) encoded route
     *
     * Use Case: Validates that OpenLR format is supported for map matching requests.
     * OpenLR is a standard for encoding location references used in automotive and telematics.
     *
     * Flow:
     * 1. Setup map matching with OpenLR encoded coordinates ("CwOiYCUMoBNWAv9P/+MSBg==")
     * 2. Request map matching to decode OpenLR and generate navigable route
     * 3. Set matched route as active and simulate vehicle movement
     * 4. Navigate to completion
     *
     * Validates:
     * - OpenLR encoded location references can be converted to navigable routes
     * - Map Matching API accepts Base64 OpenLR input
     * - Navigation completes successfully with OpenLR-derived routes
     *
     * Example: Follow a route shared via OpenLR standard (common in automotive industry)
     */
    @Test
    fun arriveOnMapMatchedRouteFromOpenLR() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
        ) { navigation ->
            val options = setupOpenLrTestRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            // Use only the first match
            navigation.setNavigationRoutes(listOf(result.matches.first().navigationRoute))
            mockLocationReplayerRule.playRoute(
                result.matches.first().navigationRoute.directionsRoute,
            )
            navigation.startTripSession()
            navigation.routeProgressUpdates().first {
                it.currentState == RouteProgressState.COMPLETE
            }
        }
    }

    /**
     * Test: Automatic route switching when deviating from map-matched route to Directions API alternative
     *
     * Use Case: Driver has a map-matched primary route (e.g., historical/preferred path) but also has
     * a Directions API alternative available. When driver follows the alternative, system should
     * automatically switch to it without making a new API request.
     *
     * Flow:
     * 1. Request both map-matched route AND Directions API route (same origin/destination)
     * 2. Set map-matched route as primary, Directions API route as alternative
     * 3. Simulate driving along the Directions API route (deviation from primary)
     * 4. System detects deviation and switches to the Directions API alternative
     * 5. Monitor reroute events to verify no new API call was made
     *
     * Validates:
     * - Mixed route alternatives (map-matched + Directions API) work together
     * - System detects deviation and switches to existing alternative route
     * - No new API request needed (uses pre-loaded alternative)
     * - Reroute state: Idle → FetchingRoute → RouteFetched(ONLINE) → Idle
     * - Routes update reason is REROUTE
     *
     * Example: Fleet driver has preferred historical route but system also provides current fastest route.
     * Driver takes the faster route → system switches without new API call.
     */
    @Test
    fun deviateToRegularRouteAlternative() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
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
                listOf(
                    mapMatchingResult.matches.first().navigationRoute,
                ) + directionsAPIResult.routes,
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

    /**
     * Test: Automatic route switching when deviating FROM Directions API route TO map-matched alternative
     *
     * Use Case: Reverse scenario where Directions API route is primary and map-matched route is alternative.
     * Tests that map-matched routes can serve as alternatives to standard routes.
     *
     * Flow:
     * 1. Request Directions API route and map-matched route for same origin/destination
     * 2. Set Directions API as primary, map-matched as alternative
     * 3. Simulate driving along the map-matched route (deviation from Directions API primary)
     * 4. System switches to map-matched alternative
     * 5. Monitor both RerouteState (V1) and RerouteStateV2 observers
     *
     * Validates:
     * - Map-matched routes can serve as alternatives to Directions API routes
     * - Switching FROM standard TO map-matched route works correctly
     * - Both RerouteState and RerouteStateV2 observers report success
     * - Regular online reroute flow is followed
     *
     * Example: Driver chooses to follow company's standard route over system-suggested fastest route
     */
    @Test
    fun deviateFromRegularToMapMatchedAlternativeRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            val rerouteStatesV2 = mutableListOf<RerouteStateV2>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            navigation.getRerouteController()!!.registerRerouteStateV2Observer {
                rerouteStatesV2.add(it)
            }

            val (options, directionOptions) =
                setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
            val directionsAPIResult =
                navigation.requestRoutes(directionOptions).getSuccessfulResultOrThrowException()
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            val setRouteResult = navigation.setNavigationRoutesAsync(
                directionsAPIResult.routes + listOf(
                    mapMatchingResult.matches.first().navigationRoute,
                ),
            )

            assertEquals(0, setRouteResult.value!!.ignoredAlternatives.size)
            mockLocationReplayerRule.playRoute(
                mapMatchingResult.matches.first().navigationRoute.directionsRoute,
            )
            navigation.startTripSession()

            val routesUpdate = navigation.routesUpdates()
                .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
            assertEquals(
                mapMatchingResult.matches.single().navigationRoute.id,
                routesUpdate.navigationRoutes.first().id,
            )
            assertEquals(
                regularOnlineRerouteFlow,
                rerouteStates,
            )
            assertSuccessfulRouteAppliedRerouteStateTransition(rerouteStatesV2)
        }
    }

    /**
     * Test: Switching between two map-matched route alternatives
     *
     * Use Case: Multiple map-matched routes exist as alternatives (e.g., different historical routes
     * for the same destination). System should support switching between them.
     *
     * Flow:
     * 1. Request TWO different map-matched routes for the same origin/destination
     * 2. Set first as primary, second as alternative
     * 3. Simulate driving along the second (alternative) route
     * 4. System should switch to the alternative map-matched route
     * 5. Monitor both V1 and V2 reroute state observers
     *
     * Validates:
     * - Multiple map-matched routes can coexist as alternatives
     * - System can switch between map-matched routes without Directions API
     * - Regular online reroute flow applies
     * - Both observer versions report successful transition
     *
     * Example: Fleet has multiple historical delivery routes (Route A from Monday, Route B from Tuesday).
     * Driver starts following Route B → system recognizes and switches.
     */
    @Test
    fun deviateToMapMatchedAlternativeRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            val rerouteStatesV2 = mutableListOf<RerouteStateV2>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            navigation.getRerouteController()!!.registerRerouteStateV2Observer {
                rerouteStatesV2.add(it)
            }

            val (primaryMapMatched, _, mapMatchedAlternativeOptions) =
                setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
            val mapMatchingPrimary =
                navigation.requestMapMatching(primaryMapMatched).getSuccessfulOrThrowException()
            val mapMatchingAlternative =
                navigation.requestMapMatching(mapMatchedAlternativeOptions)
                    .getSuccessfulOrThrowException()
            val setRouteResult = navigation.setNavigationRoutesAsync(
                listOf(mapMatchingPrimary.matches.first().navigationRoute) +
                    listOf(mapMatchingAlternative.matches.first().navigationRoute),
            )

            assertEquals(0, setRouteResult.value!!.ignoredAlternatives.size)
            mockLocationReplayerRule.playRoute(
                mapMatchingAlternative.matches.first().navigationRoute.directionsRoute,
            )
            navigation.startTripSession()

            val routesUpdate = navigation.routesUpdates()
                .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
            assertEquals(
                mapMatchingAlternative.matches.single().navigationRoute.id,
                routesUpdate.navigationRoutes.first().id,
            )
            assertEquals(
                regularOnlineRerouteFlow,
                rerouteStates,
            )
            assertSuccessfulRouteAppliedRerouteStateTransition(rerouteStatesV2)
        }
    }

    /**
     * Test: Route refresh behavior with mixed route types (Directions API + Map Matching)
     *
     * Use Case: When alternatives include both Directions API and map-matched routes, validate
     * that route refresh works correctly. Only Directions API routes should be refreshable
     * (map-matched routes are static historical data).
     *
     * Flow:
     * 1. Setup Directions API route as primary, map-matched route as alternative
     * 2. Start navigation on Directions API route
     * 3. Move along the route to enter tracking state
     * 4. Request immediate route refresh
     * 5. Monitor refresh state changes
     *
     * Validates:
     * - Route refresh works when alternatives include different route types
     * - Only Directions API routes are refreshed (get live traffic updates)
     * - Map-matched routes are ignored during refresh (they're static)
     * - Refresh succeeds with states: STARTED → FINISHED_SUCCESS
     * - Routes update with reason REFRESH
     *
     * Example: Live traffic updates for current route while having historical route as backup
     */
    @Test
    fun refreshMixedRoutes() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
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
                directionsAPIResult.routes + listOf(
                    mapMatchingResult.matches.first().navigationRoute,
                ),
            )
            assertEquals(0, setRouteResult.value!!.ignoredAlternatives.size)

            navigation.startTripSession()
            navigation.moveAlongTheRouteUntilTracking(
                directionsAPIResult.routes[0],
                mockLocationReplayerRule,
            )

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

    /**
     * Test: Going off-route on a map-matched route without fallback strategy configured
     *
     * Use Case: When navigating on a map-matched route and driver goes off-route, validate that
     * reroute FAILS when no fallback strategy is configured. Map-matched routes cannot be
     * directly rerouted via Map Matching API.
     *
     * Flow:
     * 1. Request map-matched route and set as active
     * 2. Start navigation and track normally along the route
     * 3. Simulate going off-route by staying at a position away from the route
     * 4. System detects off-route and attempts to reroute
     * 5. Monitor both V1 and V2 reroute state observers
     *
     * Validates:
     * - Off-route detection works on map-matched routes
     * - Reroute attempts without fallback strategy FAIL
     * - Reroute states: Idle → FetchingRoute → Idle (no success)
     * - Both RerouteState and RerouteStateV2 observers report failed transition
     *
     * Example: Driver deviates from predefined route without NavigateToFinalDestination or
     * RerouteDisabled strategy configured → reroute fails, app must handle manually
     */
    @Test
    fun offRouteOnMapMatchedRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            val rerouteStatesV2 = mutableListOf<RerouteStateV2>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            navigation.getRerouteController()!!.registerRerouteStateV2Observer {
                rerouteStatesV2.add(it)
            }
            val options = setupTestMapMatchingRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            navigation.setNavigationRoutes(listOf(result.matches.first().navigationRoute))
            navigation.startTripSession()
            navigation.moveAlongTheRouteUntilTracking(
                result.matches.first().navigationRoute,
                mockLocationReplayerRule,
            )

            stayOnPosition(
                latitude = 32.712997,
                longitude = -117.172881,
                bearing = 178.0f,
                frequencyHz = 5,
            ) {
                navigation.getRerouteController()?.rerouteStates()?.first {
                    it is RerouteState.FetchingRoute
                }
                navigation.getRerouteController()?.rerouteStatesV2()?.first {
                    it is RerouteStateV2.FetchingRoute
                }
                navigation.getRerouteController()?.rerouteStates()?.first {
                    it is RerouteState.Idle
                }
                navigation.getRerouteController()?.rerouteStatesV2()?.first {
                    it is RerouteStateV2.Idle
                }

                yield()

                assertRerouteFailedTransition(rerouteStates)
                assertRerouteFailedTransitionV2(rerouteStatesV2)
            }
        }
    }

    /**
     * Test: Off-route behavior with serialized/deserialized map-matched route
     *
     * Use Case: Apps often save routes to disk for offline use or session recovery. This validates
     * that serialization/deserialization preserves route metadata and off-route behavior works
     * identically to non-deserialized routes.
     *
     * Flow:
     * 1. Request map-matched route
     * 2. Serialize the route to JSON/bytes
     * 3. Deserialize back to NavigationRoute object
     * 4. Verify deserialization succeeded (no errors)
     * 5. Set deserialized route as active
     * 6. Simulate going off-route
     * 7. Monitor reroute failure (same as non-deserialized)
     *
     * Validates:
     * - Serialization/deserialization preserves route properties
     * - Deserialized map-matched routes maintain their "map-matched" identity
     * - Off-route detection works identically with deserialized routes
     * - Reroute fails appropriately (no fallback strategy)
     * - Both observer versions report failed transition
     *
     * Example: Route persistence across app restarts
     * - Session 1: Get route, serialize, save to disk
     * - Session 2: Load, deserialize, navigate → same behavior as original
     */
    @Test
    fun offRouteOnDeserializedMapMatchedRoute() = sdkTest {
        assumeFalse(
            "this test didn't work great with native reroute, so we disable it because" +
                "deserialization maybe going to be removed " +
                "https://mapbox.atlassian.net/browse/NAVAND-6775 anyway",
            runOptions.nativeReroute,
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            val rerouteStatesV2 = mutableListOf<RerouteStateV2>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            navigation.getRerouteController()!!.registerRerouteStateV2Observer {
                rerouteStatesV2.add(it)
            }
            val options = setupTestMapMatchingRoute()
            val result = navigation.requestMapMatching(options).getSuccessfulOrThrowException()

            val deserializationResult = withContext(Dispatchers.Default) {
                deserializeNavigationRouteFrom(
                    result.matches.first().navigationRoute.serialize(),
                )
            }
            assertNull(
                deserializationResult.error,
            )
            val deserializedRoute = deserializationResult.value!!

            navigation.setNavigationRoutes(listOf(deserializedRoute))

            navigation.startTripSession()
            navigation.moveAlongTheRouteUntilTracking(
                result.matches.first().navigationRoute,
                mockLocationReplayerRule,
            )

            stayOnPosition(
                latitude = 32.712997,
                longitude = -117.172881,
                bearing = 178.0f,
                frequencyHz = 5,
            ) {
                navigation.getRerouteController()?.rerouteStates()?.first {
                    it is RerouteState.FetchingRoute
                }
                navigation.getRerouteController()?.rerouteStatesV2()?.first {
                    it is RerouteStateV2.FetchingRoute
                }
                navigation.getRerouteController()?.rerouteStates()?.first {
                    it is RerouteState.Idle
                }
                navigation.getRerouteController()?.rerouteStatesV2()?.first {
                    it is RerouteStateV2.Idle
                }

                // let the rerouteStatesV2Observer be notified
                yield()

                assertRerouteFailedTransition(rerouteStates)
                assertRerouteFailedTransitionV2(rerouteStatesV2)
            }
        }
    }

    /**
     * Test: Off-route with NavigateToFinalDestination strategy (single-leg route)
     *
     * Use Case: When configured with NavigateToFinalDestination strategy, the system should fall back
     * to Directions API when driver goes off a map-matched route, preserving the final destination.
     *
     * Flow:
     * 1. Configure rerouteStrategyForMapMatchedRoutes = NavigateToFinalDestination
     * 2. Request map-matched route (single leg: origin → destination, 2 waypoints)
     * 3. Set as active route
     * 4. Simulate deviation from the route
     * 5. System falls back to Directions API to create new route
     * 6. Verify new route targets same final destination
     *
     * Validates:
     * - Fallback strategy activates when driver goes off-route
     * - New route comes from Directions API (not Map Matching)
     * - Route has 2 waypoints: current_position → original_destination
     * - Final destination is preserved within 10 meters (GPS snapping tolerance)
     * - Reroute succeeds: Idle → FetchingRoute → RouteFetched(ONLINE) → Idle
     *
     * Example: Delivery driver with predefined route deviates
     * - Original: Map-matched route (Warehouse → Customer)
     * - Deviation: Driver takes different path
     * - Fallback: Directions API creates (current_location → Customer)
     * - Result: Still reaches the same customer location
     */
    @Test
    fun offRouteOnCustomMapMatchedRouteFallbackToDirectionsApiAndFinalDestination() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
            rerouteStrategyForMapMatchedRoutes = NavigateToFinalDestination,
        ) { navigation ->
            val geometryToDeviate = setupMockRouteAfterDeviation()

            val rerouteStates = mutableListOf<RerouteState>()
            val rerouteStatesV2 = mutableListOf<RerouteStateV2>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            navigation.getRerouteController()!!.registerRerouteStateV2Observer {
                rerouteStatesV2.add(it)
            }

            val options = setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
                .primaryMapMatchedRouteOptions
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            // use only the first MM route as the primary
            navigation.setNavigationRoutes(
                listOf(mapMatchingResult.matches.first().navigationRoute),
            )
            // replay Directions route just to trigger off route event
            mockLocationReplayerRule.playGeometry(geometryToDeviate)
            navigation.startTripSession()

            val newRoute: NavigationRoute = navigation.routesUpdates()
                .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
                .navigationRoutes
                .first()

            // DirectionsAPI is used as a fallback for a MM route
            assertEquals(DIRECTIONS_API, newRoute.responseOriginAPI)
            assertEquals(2, newRoute.waypoints?.size)

            // Directions API could slightly move destination due to snapping
            val expectedDestination = mapMatchingResult.matches
                .first()
                .navigationRoute
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
            assertSuccessfulRouteAppliedRerouteStateTransition(rerouteStatesV2)
        }
    }

    /**
     *   Scenario: When navigating on a multi-leg map-matched route (a route with intermediate waypoints)
     *   and the driver deviates from the route, the navigation system needs to handle rerouting differently
     *   than with standard Directions API routes.
     *
     *   Key Behavior:
     *
     *   1. Fallback to Directions API
     *     - Map-matched routes cannot be directly rerouted via the Map Matching API
     *     - The system must fall back to the standard Directions API for rerouting
     *     - Assertion: newRoute.responseOriginAPI == DIRECTIONS_API
     *   2. Navigate to Final Destination Only
     *     - When using NavigateToFinalDestination strategy, unvisited intermediate waypoints are dropped
     *     - Original route had 3 waypoints (origin → waypoint → destination)
     *     - After reroute, the new route only has 2 waypoints (current position → final destination)
     *     - Assertion: newRoute.waypoints?.size == 2 (down from 3)
     *   3. Preserve Final Destination
     *     - The final destination coordinate is preserved (with acceptable GPS snapping tolerance)
     *     - Verifies the destination is within 10 meters of the original
     *     - Uses Turf measurement to calculate distance
     *   4. Successful Reroute Flow
     *     - Confirms the reroute state machine transitions correctly
     *     - Shows online reroute succeeded
     *
     *   Configuration
     *
     *   This test uses rerouteStrategyForMapMatchedRoutes = NavigateToFinalDestination, which is specifically designed for scenarios where:
     *   - Routes come from map-matching (e.g., replaying historical traces, following predefined paths)
     *   - When off-route occurs, the user still wants to reach the final destination
     *   - Intermediate stops can be skipped if the route is lost
     */
    @Test
    fun offRouteOnMapMatchedRouteFallbackToDirectionsApiAndFinalDestinationMultiLeg() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
            rerouteStrategyForMapMatchedRoutes = NavigateToFinalDestination,
        ) { navigation ->
            val geometryToDeviate = setupMockRouteAfterDeviation()

            val rerouteStates = mutableListOf<RerouteState>()
            val rerouteStatesV2 = mutableListOf<RerouteStateV2>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            navigation.getRerouteController()!!.registerRerouteStateV2Observer {
                rerouteStatesV2.add(it)
            }

            val options = setupTwoLegsMapMatchingRoute()
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            navigation.setNavigationRoutes(
                listOf(mapMatchingResult.matches.first().navigationRoute),
            )
            val initialPrimaryRoute = mapMatchingResult.matches.first().navigationRoute
            assertEquals(
                3,
                initialPrimaryRoute.waypoints?.size,
            )

            // geometry that deviates from the route and goes to the destination
            mockLocationReplayerRule.playGeometry(geometryToDeviate)
            navigation.startTripSession()

            val newRoute: NavigationRoute = navigation.routesUpdates()
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
            val expectedDestination = mapMatchingResult.matches
                .first()
                .navigationRoute
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
            assertSuccessfulRouteAppliedRerouteStateTransition(rerouteStatesV2)
        }
    }

    /**
     * Test: Off-route with RerouteDisabled strategy
     *
     * Use Case: When rerouting must be completely disabled (e.g., autonomous vehicle testing,
     * strict route adherence), validate that off-route is detected but reroute attempts fail
     * with appropriate error message.
     *
     * Flow:
     * 1. Configure rerouteStrategyForMapMatchedRoutes = RerouteDisabled
     * 2. Request map-matched route and set as active
     * 3. Simulate deviation by replaying different route geometry
     * 4. Register off-route observer to detect when deviation occurs
     * 5. Verify reroute is attempted but fails with appropriate message
     *
     * Validates:
     * - Rerouting is completely disabled when configured
     * - Off-route is still detected (off-route observer fires)
     * - Reroute states: Idle → FetchingRoute → Failed → Idle
     * - Error message: "new routes calculation for routes from Mapbox Map Matching API is disabled"
     * - Both V1 and V2 observers report failure with message
     * - Application must handle rerouting manually
     *
     * Example: Autonomous vehicle testing with predefined test path
     * - Route: Strict test path that must be followed exactly
     * - Policy: No automatic deviations allowed
     * - Deviation: Vehicle goes off-route
     * - Result: Off-route detected but reroute disabled, app must intervene
     */
    @Test
    fun offRouteOnCustomMapMatchedRouteFailsOnRerouteDisabledStrategy() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
            rerouteStrategyForMapMatchedRoutes = RerouteDisabled,
        ) { navigation ->
            val rerouteStates = mutableListOf<RerouteState>()
            val rerouteStatesV2 = mutableListOf<RerouteStateV2>()
            navigation.getRerouteController()!!.registerRerouteStateObserver {
                rerouteStates.add(it)
            }
            navigation.getRerouteController()!!.registerRerouteStateV2Observer {
                rerouteStatesV2.add(it)
            }

            val (options, directionOptions) =
                setupAlternativeRoutesFromMapMatchingAndDirectionsAPI()
            val directionsAPIResult =
                navigation.requestRoutes(directionOptions).getSuccessfulResultOrThrowException()
            val mapMatchingResult =
                navigation.requestMapMatching(options).getSuccessfulOrThrowException()
            navigation.setNavigationRoutes(
                listOf(mapMatchingResult.matches.first().navigationRoute),
            )
            // replay Directions route just to trigger off route event
            mockLocationReplayerRule.playRoute(directionsAPIResult.routes.first().directionsRoute)
            navigation.startTripSession()

            navigation.getRerouteController()?.rerouteStates()?.first {
                it is RerouteState.FetchingRoute
            }
            navigation.getRerouteController()?.rerouteStatesV2()?.first {
                it is RerouteStateV2.FetchingRoute
            }
            navigation.getRerouteController()?.rerouteStates()?.first {
                it is RerouteState.Idle
            }
            navigation.getRerouteController()?.rerouteStatesV2()?.first {
                it is RerouteStateV2.Idle
            }

            yield()

            assertRerouteFailedTransition(rerouteStates)
            assertRerouteFailedTransitionV2(rerouteStatesV2)
        }
    }

    /**
     * Test: Route refresh attempts on map-matched routes (should fail)
     *
     * Use Case: Validate that map-matched routes cannot be refreshed because they represent
     * static historical data, not live routes with traffic. System should fail gracefully
     * without retries.
     *
     * Flow:
     * 1. Configure route refresh with 1-second interval
     * 2. Request map-matched route and set as active
     * 3. Start navigation and track along route
     * 4. Wait for automatic refresh attempt (after 2 seconds)
     * 5. Verify refresh fails
     * 6. Wait to verify no retry happens
     * 7. Request manual immediate refresh
     * 8. Verify manual refresh also fails
     *
     * Validates:
     * - Map-matched routes CANNOT be refreshed (they're static)
     * - Automatic refresh: STARTED → FINISHED_FAILED
     * - No automatic retry for map-matched routes
     * - Manual refresh also fails with same states
     * - Prevents unnecessary API calls
     *
     * Example: System attempts to get traffic updates for historical route
     * - Route: Map-matched historical delivery path
     * - Auto-refresh triggers: Every 1 second
     * - Result: Each attempt fails (no live traffic data for historical routes)
     * - System: Doesn't retry, saves API quota
     *
     * Why Important: Clarifies that map-matched routes are immutable and prevents
     * wasted API calls for features that don't apply.
     */
    @Test
    fun refreshOfMapMatchedRoute() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
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
            navigation.setNavigationRoutes(listOf(result.matches.first().navigationRoute))
            navigation.startTripSession()
            navigation.moveAlongTheRouteUntilTracking(
                result.matches[0].navigationRoute,
                mockLocationReplayerRule,
            )
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

    /**
     * Test: Error handling when map matching fails to find road segments
     *
     * Use Case: When GPS coordinates are too far from any road network (e.g., in ocean, desert,
     * or remote areas), the Map Matching API returns "NoSegment" error. Validate proper error handling.
     *
     * Flow:
     * 1. Mock server to return "no segments found" error response (200 OK with error body)
     * 2. Request map matching with coordinates that can't be matched to roads
     * 3. Verify result is MapMatchingRequestResult.Failure
     *
     * Validates:
     * - API error responses are properly handled
     * - "NoSegment" error (coordinates too far from roads) is caught
     * - Returns failure result instead of crashing
     * - App can gracefully handle unmatchable coordinates
     *
     * Example: GPS coordinates in ocean or desert where no roads exist
     * - Input: Ocean coordinates (-71.443158, 39.613564 to -71.448504, 39.596188)
     * - Map Matching: Can't find nearby road segments
     * - Response: HTTP 200 with "NoSegment" error code
     * - App: Receives failure, can show user error message
     */
    @Test
    fun no_segments_found_error() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
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
            navigation.requestMapMatching(options) as MapMatchingRequestResult.Failure
        }
    }

    /**
     * Test: Authentication failure handling with invalid access token
     *
     * Use Case: Validate graceful handling when API requests fail due to invalid,
     * expired, or wrong access token (401 Unauthorized).
     *
     * Flow:
     * 1. Set wrong/invalid access token (TEST_WRONG_TOKEN = "wrong-token")
     * 2. Request map matching with this invalid token
     * 3. Verify result is MapMatchingRequestResult.Failure
     *
     * Validates:
     * - Unauthorized (401) errors are handled gracefully
     * - Invalid tokens don't crash the app
     * - Returns failure result with appropriate error
     * - App can prompt user to check API credentials
     *
     * Example: Invalid or expired API token
     * - Token: "wrong-token" (not a valid Mapbox token)
     * - Request: Map matching
     * - Response: 401 Unauthorized
     * - App: Gracefully handles failure, can show auth error to user
     */
    @Test
    fun unauthorised() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
        ) { navigation ->
            MapboxOptions.accessToken = TEST_WRONG_TOKEN
            val options = setupTestMapMatchingRoute()
            navigation.requestMapMatching(options) as MapMatchingRequestResult.Failure
        }
    }

    /**
     * Test: Network unavailability handling
     *
     * Use Case: Validate graceful handling when device has no internet connectivity
     * and map matching request cannot reach the server.
     *
     * Flow:
     * 1. Disable internet connection using withoutInternet helper
     * 2. Attempt to request map matching
     * 3. Verify result is MapMatchingRequestResult.Failure
     *
     * Validates:
     * - Network errors are handled gracefully
     * - App doesn't crash without connectivity
     * - Returns failure result for offline requests
     * - App can detect and inform user about connectivity issues
     *
     * Example: Device loses connectivity
     * - Network: Disabled/unavailable (airplane mode, no WiFi/data)
     * - Request: Map matching
     * - Result: Network failure (cannot reach server)
     * - App: Returns failure, can show "No internet" message
     */
    @Test
    fun noInternet() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
        ) { navigation ->
            val options = setupTestMapMatchingRoute()
            withoutInternet {
                navigation.requestMapMatching(options)
                    as MapMatchingRequestResult.Failure
            }
        }
    }

    /**
     * Test: Cancelling in-flight map matching requests
     *
     * Use Case: User cancels navigation setup or changes plans before route loads.
     * Validate that in-flight API requests can be properly cancelled.
     *
     * Flow:
     * 1. Start map matching request with callback
     * 2. Immediately cancel using cancelMapMatchingRequest(requestId)
     * 3. Monitor callback invocations
     * 4. Wait for onCancel() callback to complete
     *
     * Validates:
     * - Requests can be cancelled mid-flight
     * - onCancel() callback IS invoked
     * - success() callback is NOT called
     * - failure() callback is NOT called
     * - Request ID system works correctly
     * - Prevents wasted processing and network usage
     *
     * Example: User cancels navigation setup before route loads
     * - User action: Clicks "Start Navigation" → route loading begins
     * - User action: Clicks "Cancel" button → app cancels request
     * - System: Calls cancelMapMatchingRequest(requestId)
     * - Result: onCancel() fires, no success/failure callbacks
     * - Benefit: Saves network bandwidth and API quota
     */
    @Test
    fun requestCancellation() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
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

    /**
     * Test: Cleanup when navigation instance is destroyed during active request
     *
     * Use Case: Validate proper lifecycle management when navigation is destroyed
     * (e.g., app closed, activity destroyed) while map matching request is in-flight.
     * Critical for preventing memory leaks and dangling callbacks.
     *
     * Flow:
     * 1. Start map matching request with callback
     * 2. While request is in-flight, destroy navigation instance via MapboxNavigationProvider.destroy()
     * 3. Monitor callback invocations
     * 4. Verify onCancel() is called
     *
     * Validates:
     * - Destroying navigation cancels all pending requests
     * - Memory leaks are prevented (no dangling callbacks)
     * - onCancel() callback IS invoked during cleanup
     * - success() and failure() callbacks are NOT called
     * - Proper lifecycle management in Android
     * - Resources are cleaned up correctly
     *
     * Example: App is destroyed during route loading
     * - State: Map matching request in progress
     * - User action: Presses back button / kills app
     * - System: Android destroys activity → MapboxNavigationProvider.destroy()
     * - Result: Request cancelled, onCancel() called
     * - Benefit: Prevents memory leaks, orphaned callbacks, and crashes
     *
     * Why Critical: In Android, activities can be destroyed at any time. Proper cleanup
     * prevents crashes from callbacks firing after components are destroyed.
     */
    @Test
    fun destroyNavigation() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
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
