@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStore
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.ChargingState
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.directions.session.RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.offRouteUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.getTestRerouteCustomConfig
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.offline.Tileset
import com.mapbox.navigation.testing.utils.offline.unpackTiles
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.EvRoutesProvider
import com.mapbox.navigation.testing.utils.routes.MockedEvRouteWithSingleUserProvidedChargingStation
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import com.mapbox.navigation.testing.R as TestResourcesR

/**
 * Rerouting and replanning while charging at a departure charging station: charging being
 * expected must not suppress a real reroute once the vehicle drives away, the rerouted-to route
 * must carry its own charging state from its first progress, and a replan or back-online
 * request made while plugged in must keep describing the charger at the origin.
 */
class ChargingAtDepartureRerouteTest : BaseCoreNoCleanUpTest() {

    companion object {

        /**
         * Deviation used once the vehicle is moving: unambiguously off the road, so that the run
         * fails on the reroute being suppressed rather than on the deviation being too subtle.
         */
        private const val MOVING_DEVIATION_METERS = 200.0

        private const val REROUTE_TIMEOUT_MILLIS = 30_000L

        private const val KEY_REASON = "reason"
        private const val VALUE_PARAMETERS_CHANGE = "parameters_change"
        private const val POLLING_INTERVAL_MILLIS = 100L
        private const val KEY_EV_INITIAL_CHARGE = "ev_initial_charge"
        private const val KEY_WAYPOINTS_STATION_ID = "waypoints.charging_station_id"
        private const val KEY_WAYPOINTS_POWER = "waypoints.charging_station_power"
        private const val KEY_WAYPOINTS_CURRENT_TYPE = "waypoints.charging_station_current_type"

        /**
         * State of charge reported once the vehicle has been charging for a while at the
         * departure charger. Distinct from the `ev_initial_charge` the route was requested with,
         * so an assertion on it can't accidentally pass against the original request.
         */
        private const val CHARGED_STATE_OF_CHARGE = "35000"

        /** Coordinates in a Directions request URL are rounded, so compare with a tolerance. */
        private const val COORDINATE_TOLERANCE = 1e-4
    }

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private val evRouteOrigin by lazy {
        EvRoutesProvider.getBerlinEvRouteWithChargingAtDeparture(context).origin
    }

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = evRouteOrigin.latitude()
            longitude = evRouteOrigin.longitude()
        }
    }

    /**
     * Charging-expected is not cleared by driving away from the charging station without
     * unplugging, but once the vehicle is moving a deviation is a real deviation: the engine
     * reports it off-route and it must be rerouted, exactly as it would be with no charging in
     * progress. Both reroute controllers must agree on that. The very first route progress under
     * the rerouted-to route already carries that route's charging state, not the previous route's.
     */
    @Test
    fun deviationWhileMovingWithChargingStillExpectedIsRerouted() = sdkTest {
        val evRoute = EvRoutesProvider.getBerlinEvRouteWithChargingAtDeparture(
            context,
            mockWebServerRule.baseUrl,
        )
        mockWebServerRule.requestHandlers.add(evRoute.mockWebServerHandler)
        // Serves whatever the reroute asks for: the deviation point is derived from where the
        // vehicle happens to be map-matched mid-drive, so it can't be spelled out up front.
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(
                    context,
                    TestResourcesR.raw.ev_routes_berlin_reroute,
                ),
                expectedCoordinates = null,
                relaxedExpectedCoordinates = true,
            ),
        )

        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestRerouteCustomConfig(),
        ) { navigation ->
            val routes = navigation.requestRoutes(evRoute.routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            val originalRouteId = routes.first().id
            navigation.startTripSession()

            val initialProgressDeferred = async {
                navigation.routeProgressUpdates()
                    .first { it.currentState == RouteProgressState.INITIALIZED }
            }
            stayOnPosition(evRoute.origin.latitude(), evRoute.origin.longitude(), bearing = 0f) {
                navigation.setNavigationRoutesAsync(routes)
                initialProgressDeferred.await()
                navigation.routeProgressUpdates()
                    .first { it.chargingState == ChargingState.AWAIT_CHARGING }
                navigation.startCharging()
                navigation.routeProgressUpdates()
                    .first { it.chargingState == ChargingState.CHARGING }
            }

            // Subscribed before the deviation, so the first progress under the new route can't be
            // missed by subscribing only after the reroute has been observed.
            val firstProgressUnderNewRoute = async {
                navigation.routeProgressUpdates()
                    .first { it.navigationRoute.id != originalRouteId }
            }

            // Drive away still plugged in: route following goes to TRACKING while charging is
            // still expected.
            mockLocationReplayerRule.playRoute(routes.first().directionsRoute)
            val trackingProgress = navigation.routeProgressUpdates()
                .first { it.currentState == RouteProgressState.TRACKING }
            assertTrue(trackingProgress.isChargingExpected)
            mockLocationReplayerRule.stopAndClearEvents()

            val deviationFrom = trackingProgress.currentLegProgress
                ?.currentStepProgress?.stepPoints?.firstOrNull() ?: evRoute.origin
            val deviationPoint = TurfMeasurement.destination(
                deviationFrom,
                MOVING_DEVIATION_METERS,
                90.0,
                TurfConstants.UNIT_METERS,
            )

            val rerouteResult = stayOnPosition(
                deviationPoint.latitude(),
                deviationPoint.longitude(),
                bearing = 90f,
            ) {
                withTimeout(REROUTE_TIMEOUT_MILLIS.milliseconds) {
                    navigation.offRouteUpdates().first { it }
                }
                withTimeout(REROUTE_TIMEOUT_MILLIS.milliseconds) {
                    navigation.routesUpdates().first {
                        it.reason == ROUTES_UPDATE_REASON_REROUTE
                    }
                }
            }

            // Precondition for the assertion below: the new route's own origin is not a charging
            // station, so the charging-expected signal that follows can't be explained by the new
            // route happening to need charging too - it must come from somewhere else.
            val newRouteOrigin = rerouteResult.navigationRoutes.first().internalWaypoints()
                .firstOrNull()
            assertNotNull(
                "expected the rerouted-to route to have at least one waypoint",
                newRouteOrigin,
            )
            assertTrue(
                "test setup problem: the rerouted-to route must not have a charging station " +
                    "at its own origin, waypoint type: ${newRouteOrigin?.type}",
                newRouteOrigin?.type != Waypoint.EV_CHARGING_SERVER &&
                    newRouteOrigin?.type != Waypoint.EV_CHARGING_USER,
            )

            // The vehicle is still plugged in, so the native FSM keeps reporting charging, but
            // downgrades it to EXTRA_CHARGING once the current waypoint isn't a charging station.
            val postRerouteProgress = withTimeout(REROUTE_TIMEOUT_MILLIS.milliseconds) {
                firstProgressUnderNewRoute.await()
            }
            assertTrue(postRerouteProgress.isChargingExpected)
            assertEquals(ChargingState.EXTRA_CHARGING, postRerouteProgress.chargingState)
        }
    }

    /**
     * The driver charges at their own charger at the origin - a user-provided charging station,
     * which the server would never insert on its own - and changes a route option while plugged
     * in, which replans the route. The replan request must still describe that charging station,
     * pinned to the origin coordinate, and must carry the state of charge accumulated since the
     * original request. Losing either would send the driver a route computed as if they were
     * neither charging nor charged.
     */
    @Test
    @Ignore("blocked by NN-5489")
    fun replanWhileChargingAtDepartureKeepsUserProvidedChargingStationAtOrigin() =
        sdkTest {
            val evRoute = EvRoutesProvider
                .getBerlinEvRouteWithUserProvidedChargingStationAtDeparture(
                    context,
                    mockWebServerRule.baseUrl,
                )
            mockWebServerRule.requestHandlers.add(evRoute.mockWebServerHandler)
            // Serves every request the session makes beyond the original one - the replan
            // coordinates are derived from where the vehicle is matched, so they can't be spelled
            // out up front. Registered after the strict handler, which keeps serving the original
            // request; continuous alternatives land here too, hence the reason-based filtering of
            // the captured requests below.
            val requestHandler = MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(
                    context,
                    TestResourcesR.raw.ev_routes_berlin_user_provided_charging_station,
                ),
                expectedCoordinates = null,
                relaxedExpectedCoordinates = true,
            )
            mockWebServerRule.requestHandlers.add(requestHandler)

            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule,
                customConfig = getTestRerouteCustomConfig(),
            ) { navigation ->
                val routes = navigation.requestRoutes(evRoute.routeOptions)
                    .getSuccessfulResultOrThrowException()
                    .routes
                navigation.startTripSession()

                var replanUrl: HttpUrl? = null
                val initialProgressDeferred = async {
                    navigation.routeProgressUpdates()
                        .first { it.currentState == RouteProgressState.INITIALIZED }
                }
                stayOnPosition(
                    evRoute.origin.latitude(),
                    evRoute.origin.longitude(),
                    bearing = evRoute.originBearing,
                ) {
                    navigation.setNavigationRoutesAsync(routes)
                    initialProgressDeferred.await()
                    navigation.routeProgressUpdates()
                        .first { it.chargingState == ChargingState.AWAIT_CHARGING }
                    navigation.startCharging()
                    navigation.routeProgressUpdates()
                        .first { it.chargingState == ChargingState.CHARGING }

                    // The battery has been filling up since the route was requested. Reporting it
                    // before the replan is what makes the assertion below specific to the replan
                    // request rather than to the original one.
                    navigation.onEVDataUpdated(
                        mapOf(KEY_EV_INITIAL_CHARGE to CHARGED_STATE_OF_CHARGE),
                    )

                    // Stands for any route option the driver can change mid-session (language,
                    // exclusions, ...): what matters here is that it goes through the replan path.
                    navigation.replanRoute()
                    replanUrl = requestHandler.awaitReplanRequestUrl()
                }
                val url = replanUrl!!
                assertNotEquals(
                    "the replan must report the charge accumulated while plugged in, " +
                        "url: $url",
                    evRoute.routeOptions.unrecognizedJsonProperties
                        ?.get(KEY_EV_INITIAL_CHARGE)?.asString,
                    url.queryParameter(KEY_EV_INITIAL_CHARGE),
                )
                assertEquals(
                    "unexpected state of charge in the replan request, url: $url",
                    CHARGED_STATE_OF_CHARGE,
                    url.queryParameter(KEY_EV_INITIAL_CHARGE),
                )
                assertUserProvidedChargingStationRequestedAtOrigin(url, evRoute)
            }
        }

    /**
     * The driver's phone has no connectivity at all when they plug in at their own charger, so
     * the very first route is computed onboard. Continuous alternatives are deliberately kept
     * running while charging at departure, specifically so that once
     * connectivity returns, the "back online" mechanism can autonomously fetch and promote an
     * online route without the driver having to do anything. That online request must still
     * describe the user-provided charging station pinned to the origin, and must carry the state
     * of charge accumulated while offline - otherwise the promoted route is computed as if the
     * driver were neither charging nor charged.
     */
    @Test
    @Ignore("blocked by NN-5490")
    fun backOnlinePromotesOnlineRouteKeepingUserProvidedChargingStationAtOrigin() =
        sdkTest {
            val evRoute = EvRoutesProvider
                .getBerlinEvRouteWithUserProvidedChargingStationAtDeparture(
                    context,
                    mockWebServerRule.baseUrl,
                )
            val navigationTilesVersion =
                context.unpackTiles(Tileset.Berlin)[TileDataDomain.NAVIGATION]

            // Serves the request the back-online mechanism makes once connectivity returns - its
            // coordinates are derived from wherever the vehicle is matched, so they can't be
            // spelled out up front. Registered before the offline window below, and stays
            // registered across the mock server restart that ends it.
            val backOnlineRequestHandler = MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(
                    context,
                    TestResourcesR.raw.ev_routes_berlin_user_provided_charging_station,
                ),
                expectedCoordinates = null,
                relaxedExpectedCoordinates = true,
            )
            mockWebServerRule.requestHandlers.add(backOnlineRequestHandler)

            withMapboxNavigation(
                historyRecorderRule = mapboxHistoryTestRule,
                tileStore = TileStore.create(),
                tilesVersion = navigationTilesVersion,
                customConfig = getTestRerouteCustomConfig(),
            ) { navigation ->
                stayOnPosition(
                    evRoute.origin.latitude(),
                    evRoute.origin.longitude(),
                    bearing = evRoute.originBearing,
                ) {
                    withoutInternet {
                        val routes = navigation.requestRoutes(evRoute.routeOptions)
                            .getSuccessfulResultOrThrowException()
                            .routes
                        assertEquals(
                            "expected the first route to be computed onboard, " +
                                "with no connectivity available",
                            RouterOrigin.OFFLINE,
                            routes.first().origin,
                        )

                        val initialProgressDeferred = async {
                            navigation.routeProgressUpdates()
                                .first { it.currentState == RouteProgressState.INITIALIZED }
                        }
                        navigation.setNavigationRoutesAsync(routes)
                        navigation.startTripSession()
                        initialProgressDeferred.await()
                        navigation.routeProgressUpdates()
                            .first { it.chargingState == ChargingState.AWAIT_CHARGING }
                        navigation.startCharging()
                        navigation.routeProgressUpdates()
                            .first { it.chargingState == ChargingState.CHARGING }

                        // The battery has been filling up while offline. Reporting it before
                        // connectivity returns is what makes the assertion below specific to the
                        // back-online request rather than to a stale value.
                        navigation.onEVDataUpdated(
                            mapOf(KEY_EV_INITIAL_CHARGE to CHARGED_STATE_OF_CHARGE),
                        )
                    }
                    // Connectivity is back from here on; the mock web server has been restarted.

                    withTimeout(REROUTE_TIMEOUT_MILLIS.milliseconds) {
                        navigation.routesUpdates().first {
                            it.navigationRoutes.first().origin == RouterOrigin.ONLINE
                        }
                    }

                    val url = backOnlineRequestHandler.handledRequests
                        .mapNotNull { it.requestUrl }
                        .last()
                    assertEquals(
                        "unexpected state of charge in the back-online request, url: $url",
                        CHARGED_STATE_OF_CHARGE,
                        url.queryParameter(KEY_EV_INITIAL_CHARGE),
                    )
                    assertUserProvidedChargingStationRequestedAtOrigin(url, evRoute)

                    // Promotion must not have silently ended charging: the vehicle is still
                    // plugged in at the same station.
                    assertTrue(navigation.routeProgressUpdates().first().isChargingExpected)
                }
            }
        }

    /**
     * A replan is not the only request the session makes while parked - continuous alternatives
     * are also fetched - so requests are told apart by the reroute reason both controllers tag
     * them with, rather than by arrival order.
     */
    private suspend fun MockDirectionsRequestHandler.awaitReplanRequestUrl(): HttpUrl {
        val deadline = System.currentTimeMillis() + REROUTE_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val replanUrl = handledRequests
                .mapNotNull { it.requestUrl }
                .lastOrNull { it.queryParameter(KEY_REASON) == VALUE_PARAMETERS_CHANGE }
            if (replanUrl != null) {
                return replanUrl
            }
            delay(POLLING_INTERVAL_MILLIS.milliseconds)
        }
        throw AssertionError(
            "no replan request observed, requests made: " +
                handledRequests.map { it.requestUrl },
        )
    }

    /**
     * Asserts that [url] still requests the user-provided charging station, and that it is aligned
     * with the coordinate of the origin charger rather than shifted onto another waypoint - the
     * charging-station parameters are semicolon-separated lists positional to the coordinates.
     */
    private fun assertUserProvidedChargingStationRequestedAtOrigin(
        url: HttpUrl,
        evRoute: MockedEvRouteWithSingleUserProvidedChargingStation,
    ) {
        val coordinates = url.pathSegments.last().split(";")
        val stationIdsParameter = url.queryParameter(KEY_WAYPOINTS_STATION_ID)
        assertNotNull(
            "no $KEY_WAYPOINTS_STATION_ID in the replan request, url: $url",
            stationIdsParameter,
        )
        val stationIds = stationIdsParameter!!.split(";")
        assertEquals(
            "$KEY_WAYPOINTS_STATION_ID must have one entry per coordinate, url: $url",
            coordinates.size,
            stationIds.size,
        )

        val stationIndex = stationIds.indexOf(evRoute.chargingStationId)
        assertTrue(
            "the user provided charging station is missing from the replan request, url: $url",
            stationIndex >= 0,
        )

        val stationCoordinate = coordinates[stationIndex].split(",").map { it.toDouble() }
        assertEquals(
            "the charging station moved off the origin, url: $url",
            evRoute.origin.longitude(),
            stationCoordinate[0],
            COORDINATE_TOLERANCE,
        )
        assertEquals(
            "the charging station moved off the origin, url: $url",
            evRoute.origin.latitude(),
            stationCoordinate[1],
            COORDINATE_TOLERANCE,
        )

        assertEquals(
            "unexpected charging station power, url: $url",
            evRoute.chargingStationPower.toString(),
            url.queryParameter(KEY_WAYPOINTS_POWER)?.split(";")?.getOrNull(stationIndex),
        )
        assertEquals(
            "unexpected charging station current type, url: $url",
            evRoute.currentType,
            url.queryParameter(KEY_WAYPOINTS_CURRENT_TYPE)?.split(";")?.getOrNull(stationIndex),
        )
    }
}
