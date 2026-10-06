@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.stateOfCharge
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.trip.model.ChargingState
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.refreshStates
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.EvRoutesProvider
import com.mapbox.navigation.testing.utils.setTestRouteRefreshInterval
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import com.mapbox.navigation.testing.R as TestResourcesR

/**
 * Route refresh while charging at the origin. Before charging at departure, the engine never
 * reported the origin itself as the next waypoint, so a refresh was never made from that state:
 * the route hasn't started yet, the vehicle is parked on the first leg's first geometry point,
 * and the origin is still a remaining waypoint.
 */
class ChargingAtDepartureRouteRefreshTest : BaseCoreNoCleanUpTest() {

    private companion object {
        private const val KEY_EV_INITIAL_CHARGE = "ev_initial_charge"
        private const val KEY_CURRENT_ROUTE_GEOMETRY_INDEX = "current_route_geometry_index"
        private const val KEY_STATE_OF_CHARGE = "state_of_charge"
        private const val REFRESH_INTERVAL_MILLIS = 3_000L

        /** Position of the leg index in a `/directions-refresh/v1/...` request path. */
        private const val REFRESH_PATH_LEG_INDEX_SEGMENT = 6

        /**
         * How much lower the refreshed state of charge is than the original one, so that the
         * refreshed route can be told apart from the original one.
         */
        private const val REFRESHED_STATE_OF_CHARGE_DELTA = 1
    }

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private val evRoute by lazy {
        EvRoutesProvider.getBerlinEvRouteWithUserProvidedChargingStationAtDeparture(
            context,
            mockWebServerRule.baseUrl,
        )
    }

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = evRoute.origin.latitude()
            longitude = evRoute.origin.longitude()
        }
    }

    /**
     * A refresh made while plugged in at the origin charger is requested from the very start of
     * the route, succeeds, is applied, and leaves the charging session untouched: still charging
     * at the origin, which is still the first remaining waypoint and still a charging station.
     */
    @Test
    fun routeRefreshWhileChargingAtDepartureIsAppliedAndKeepsCharging() = sdkTest {
        val originalResponse = readRawFileText(
            context,
            TestResourcesR.raw.ev_routes_berlin_user_provided_charging_station,
        )
        val refreshHandler = MockDirectionsRefreshHandler(
            testUuid = JsonParser.parseString(originalResponse).asJsonObject["uuid"].asString,
            jsonResponse = refreshResponseWithLowerStateOfCharge(originalResponse),
            // Served only from the start of the route: a refresh requested from anywhere else
            // while still parked at the origin fails, as a server would reject it.
            acceptedGeometryIndex = 0,
        )
        mockWebServerRule.requestHandlers.add(evRoute.mockWebServerHandler)
        mockWebServerRule.requestHandlers.add(refreshHandler)

        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
            routeRefreshOptions = shortRefreshInterval(),
        ) { navigation ->
            val routes = navigation.requestRoutes(evRoute.routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            val originalRoute = routes.first()
            val waypointsCount = originalRoute.internalWaypoints().size
            navigation.startTripSession()

            stayOnPosition(evRoute.origin, bearing = evRoute.originBearing) {
                navigation.plugInAtDepartureCharger(routes)

                val refreshedRoutesUpdate = async {
                    navigation.routesUpdates()
                        .first { it.reason == ROUTES_UPDATE_REASON_REFRESH }
                }
                val refreshResult = navigation.awaitRefreshStartedWhileCharging()
                assertEquals(
                    "the refresh made while charging at the origin must succeed, " +
                        "message: ${refreshResult.message}, " +
                        "requests: ${refreshHandler.handledRequests.map { it.requestUrl }}",
                    RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS,
                    refreshResult.state,
                )

                val refreshUrl = refreshHandler.handledRequests.last().requestUrl!!
                assertEquals(
                    "the refresh must be requested from the first leg, url: $refreshUrl",
                    "0",
                    refreshUrl.pathSegments[REFRESH_PATH_LEG_INDEX_SEGMENT],
                )
                assertEquals(
                    "the refresh must be requested from the start of the route, url: $refreshUrl",
                    "0",
                    refreshUrl.queryParameter(KEY_CURRENT_ROUTE_GEOMETRY_INDEX),
                )
                assertNotNull(
                    "the refresh of an EV route must carry the state of charge, url: $refreshUrl",
                    refreshUrl.queryParameter(KEY_EV_INITIAL_CHARGE),
                )

                val refreshedRoute = refreshedRoutesUpdate.await().navigationRoutes.first()
                assertEquals(
                    "the refreshed state of charge must be applied",
                    originalRoute.firstStateOfCharge() - REFRESHED_STATE_OF_CHARGE_DELTA,
                    refreshedRoute.firstStateOfCharge(),
                    0.0,
                )
                val refreshedOrigin = refreshedRoute.internalWaypoints().first()
                assertEquals(
                    "the refresh must keep the origin a user-provided charging station",
                    Waypoint.EV_CHARGING_USER,
                    refreshedOrigin.type,
                )
                assertEquals(
                    "the refresh must keep the origin charger's target charge",
                    originalRoute.internalWaypoints().first().metadata?.get("charge_to"),
                    refreshedOrigin.metadata?.get("charge_to"),
                )

                val progressAfterRefresh = navigation.routeProgressUpdates()
                    .first { it.navigationRoute.id == refreshedRoute.id }
                assertEquals(ChargingState.CHARGING, progressAfterRefresh.chargingState)
                assertTrue(progressAfterRefresh.isChargingExpected)
                assertEquals(RouteProgressState.INITIALIZED, progressAfterRefresh.currentState)
                assertEquals(
                    "the origin charger must still count as remaining after the refresh",
                    waypointsCount,
                    progressAfterRefresh.remainingWaypoints,
                )
            }
        }
    }

    private fun shortRefreshInterval(): RouteRefreshOptions =
        RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
            .build()
            .also { it.setTestRouteRefreshInterval(REFRESH_INTERVAL_MILLIS) }

    /**
     * Sets [routes] while the vehicle is parked at the charging station at their origin and plugs
     * it in, returning once the engine reports charging there.
     */
    private suspend fun MapboxNavigation.plugInAtDepartureCharger(routes: List<NavigationRoute>) {
        coroutineScope {
            val initialProgress = async {
                routeProgressUpdates()
                    .first { it.currentState == RouteProgressState.INITIALIZED }
            }
            setNavigationRoutesAsync(routes)
            initialProgress.await()
        }
        routeProgressUpdates().first { it.chargingState == ChargingState.AWAIT_CHARGING }
        startCharging()
        routeProgressUpdates().first { it.chargingState == ChargingState.CHARGING }
    }

    /**
     * Returns the outcome of the first refresh started from now on - i.e. while charging, not
     * one that may have started before plugging in.
     */
    private suspend fun MapboxNavigation.awaitRefreshStartedWhileCharging() =
        refreshStates()
            .dropWhile { it.state != RouteRefreshExtra.REFRESH_STATE_STARTED }
            .first {
                it.state == RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS ||
                    it.state == RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED
            }

    private fun NavigationRoute.firstStateOfCharge(): Double {
        val stateOfCharge = requireNotNull(
            directionsRoute.legs()?.firstOrNull()?.annotation()?.stateOfCharge(),
        ) { "expected the EV route to carry state of charge annotations" }
        return stateOfCharge.get(0)
    }

    /**
     * A refresh response for every leg of the only route in [routeResponse], from its very first
     * geometry point, with a state of charge lowered by [REFRESHED_STATE_OF_CHARGE_DELTA].
     */
    private fun refreshResponseWithLowerStateOfCharge(routeResponse: String): String {
        val route = JsonParser.parseString(routeResponse).asJsonObject["routes"]
            .asJsonArray.single().asJsonObject
        val refreshedLegs = JsonArray().apply {
            route["legs"].asJsonArray.forEach { leg ->
                val stateOfCharge = leg.asJsonObject["annotation"].asJsonObject[KEY_STATE_OF_CHARGE]
                    .asJsonArray
                val refreshedStateOfCharge = JsonArray().apply {
                    stateOfCharge.forEach { add(it.asInt - REFRESHED_STATE_OF_CHARGE_DELTA) }
                }
                add(
                    JsonObject().apply {
                        add(
                            "annotation",
                            JsonObject().apply {
                                add(KEY_STATE_OF_CHARGE, refreshedStateOfCharge)
                            },
                        )
                    },
                )
            }
        }
        return JsonObject().apply {
            addProperty("code", "Ok")
            add("route", JsonObject().apply { add("legs", refreshedLegs) })
        }.toString()
    }
}
