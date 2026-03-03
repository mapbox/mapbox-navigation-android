package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.mapmatching.MapMatchingAPICallback
import com.mapbox.navigation.core.mapmatching.MapMatchingExtras
import com.mapbox.navigation.core.mapmatching.MapMatchingFailure
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions
import com.mapbox.navigation.core.mapmatching.MapMatchingSuccessfulResult
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.MapMatchingRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestMapMatching
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.http.MockMapMatchingRequestHandler
import com.mapbox.navigation.testing.utils.http.NotAuthorizedRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.setTestRouteRefreshInterval
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

private const val TEST_WRONG_TOKEN = "wrong-token"

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
