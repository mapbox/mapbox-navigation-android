package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.route.deserializeNavigationRouteFrom
import com.mapbox.navigation.base.internal.route.serialize
import com.mapbox.navigation.base.options.NavigateToFinalDestination
import com.mapbox.navigation.base.options.RerouteDisabled
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI.Companion.DIRECTIONS_API
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.mapmatching.MapMatchingExtras
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.reroute.RerouteStateV2
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestMapMatching
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.rerouteStates
import com.mapbox.navigation.testing.ui.utils.coroutines.rerouteStatesV2
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
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val TEST_WRONG_TOKEN_REROUTE = "wrong-token"

private val regularOnlineRerouteFlow = listOf(
    RerouteState.Idle,
    RerouteState.FetchingRoute,
    RerouteState.RouteFetched(RouterOrigin.ONLINE),
    RerouteState.Idle,
)

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(Parameterized::class)
class CoreMapMatchingRerouteTests(
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
            NotAuthorizedRequestHandler(TEST_WRONG_TOKEN_REROUTE),
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
