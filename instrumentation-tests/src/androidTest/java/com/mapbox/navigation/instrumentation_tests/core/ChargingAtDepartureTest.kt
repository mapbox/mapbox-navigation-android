@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.LegWaypoint
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.ChargingState
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.ChargingFinishedData
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheCurrentRouteUntilLocation
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.routes.EvRoutesProvider
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Verifies the native charging FSM propagates into [RouteProgress.chargingState] /
 * [RouteProgress.isChargingExpected] at an EV route's departure point, end to end
 * through [MapboxTripSession] rather than mocked as in unit tests.
 */
class ChargingAtDepartureTest : BaseCoreNoCleanUpTest() {

    private companion object {
        private const val KEY_EV_INITIAL_CHARGE = "ev_initial_charge"
    }

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private val mockRoute by lazy { RoutesProvider.berlin_short_1(context) }

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
    }

    @Test
    fun chargingStateAndIsChargingExpectedReflectNativeFsmAtDeparture() =
        sdkTest {
            val evRoute = EvRoutesProvider.getBerlinEvRouteWithChargingAtDeparture(
                context,
                mockWebServerRule.baseUrl,
            )
            mockWebServerRule.requestHandlers.add(evRoute.mockWebServerHandler)

            withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { navigation ->
                val routes = navigation.requestRoutes(evRoute.routeOptions)
                    .getSuccessfulResultOrThrowException()
                    .routes
                navigation.startTripSession()
                mockLocationReplayerRule.loopUpdate(
                    mockLocationUpdatesRule.generateLocationUpdate {
                        latitude = evRoute.origin.latitude()
                        longitude = evRoute.origin.longitude()
                    },
                    times = 120,
                )

                navigation.setNavigationRoutesAsync(routes)

                // INITIALIZED must hold steady, not just on the first observed update.
                navigation.routeProgressUpdates()
                    .take(3)
                    .toList()
                    .forEach {
                        assertEquals(RouteProgressState.INITIALIZED, it.currentState)
                    }

                // Native FSM reaches AWAIT_CHARGING on its own; startCharging() only does
                // AWAIT_CHARGING -> CHARGING once "plugged in".
                val awaitChargingProgress = navigation.routeProgressUpdates()
                    .filter { it.chargingState == ChargingState.AWAIT_CHARGING }
                    .first()
                assertEquals(RouteProgressState.INITIALIZED, awaitChargingProgress.currentState)
                assertTrue(awaitChargingProgress.isChargingExpected)

                val chargingProgressDeferred = async {
                    navigation.routeProgressUpdates()
                        .filter { it.chargingState == ChargingState.CHARGING }
                        .first()
                }
                navigation.startCharging()
                val chargingProgress = chargingProgressDeferred.await()
                assertEquals(RouteProgressState.INITIALIZED, chargingProgress.currentState)
                assertTrue(chargingProgress.isChargingExpected)

                // Still under the charge threshold, so stopCharging() here goes back to
                // AWAIT_CHARGING, not NOT_CHARGING.
                val afterStopProgressDeferred = async {
                    navigation.routeProgressUpdates()
                        .filter { it.chargingState == ChargingState.AWAIT_CHARGING }
                        .first()
                }
                var chargingFinishedData: ChargingFinishedData? = null
                navigation.stopCharging { chargingFinishedData = it }
                val afterStopProgress = afterStopProgressDeferred.await()
                assertTrue(afterStopProgress.isChargingExpected)
                assertNotNull(chargingFinishedData!!.legChanged)

                // Report a charge at/above target so the FSM falls through to EXTRA_CHARGING
                // on its own; read the target from waypoint metadata instead of hardcoding it.
                val chargeTo = routes.first().originChargeTo()
                val extraChargingProgressDeferred = async {
                    navigation.routeProgressUpdates()
                        .filter { it.chargingState == ChargingState.EXTRA_CHARGING }
                        .first()
                }
                navigation.onEVDataUpdated(
                    mapOf(KEY_EV_INITIAL_CHARGE to (chargeTo + 1).toString()),
                )
                navigation.startCharging()
                val extraChargingProgress = extraChargingProgressDeferred.await()
                // Raising the charge above target also ends the INITIALIZED pin, flipping
                // currentState to TRACKING even though the vehicle never moves.
                assertEquals(RouteProgressState.TRACKING, extraChargingProgress.currentState)
                assertTrue(extraChargingProgress.isChargingExpected)
            }
        }

    /**
     * Driving away without unplugging moves [RouteProgressState] to TRACKING normally, and the
     * native FSM leaves CHARGING for EXTRA_CHARGING as soon as the vehicle is no longer "at" the
     * charging waypoint - but [RouteProgress.isChargingExpected] stays true either way, until
     * stopCharging() is actually called.
     */
    @Test
    fun drivingAwayWithoutUnpluggingWhileChargingKeepsChargingExpectedUntilStopped() =
        sdkTest {
            val evRoute = EvRoutesProvider.getBerlinEvRouteWithChargingAtDeparture(
                context,
                mockWebServerRule.baseUrl,
            )
            mockWebServerRule.requestHandlers.add(evRoute.mockWebServerHandler)

            withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { navigation ->
                val routes = navigation.requestRoutes(evRoute.routeOptions)
                    .getSuccessfulResultOrThrowException()
                    .routes
                navigation.startTripSession()
                mockLocationReplayerRule.loopUpdate(
                    mockLocationUpdatesRule.generateLocationUpdate {
                        latitude = evRoute.origin.latitude()
                        longitude = evRoute.origin.longitude()
                    },
                    times = 120,
                )

                navigation.setNavigationRoutesAsync(routes)

                // Wait for AWAIT_CHARGING before calling startCharging() to avoid racing the FSM.
                navigation.routeProgressUpdates()
                    .filter { it.chargingState == ChargingState.AWAIT_CHARGING }
                    .first()

                val chargingProgressDeferred = async {
                    navigation.routeProgressUpdates()
                        .filter { it.chargingState == ChargingState.CHARGING }
                        .first()
                }
                navigation.startCharging()
                val chargingProgress = chargingProgressDeferred.await()

                // Drive away from the departure point while still "plugged in".
                val whileDrivingAwayProgressDeferred = async {
                    navigation.routeProgressUpdates()
                        .filter { it.currentState == RouteProgressState.TRACKING }
                        .first()
                }
                mockLocationReplayerRule.playRoute(routes.first().directionsRoute)
                val whileDrivingAwayProgress = whileDrivingAwayProgressDeferred.await()
                assertTrue(whileDrivingAwayProgress.isChargingExpected)
                assertTrue(
                    whileDrivingAwayProgress.distanceTraveled > chargingProgress.distanceTraveled,
                )
                // No longer "at" the charging waypoint, so the native FSM has already moved on
                // from CHARGING to EXTRA_CHARGING, even though the charger is still connected.
                val currentChargingState = navigation.routeProgressUpdates().first().chargingState
                assertEquals(ChargingState.EXTRA_CHARGING, currentChargingState)

                // Unplug mid-drive: charging-expected should flip off regardless of route state.
                val afterChargingProgressDeferred = async {
                    navigation.routeProgressUpdates()
                        .filter { it.chargingState == ChargingState.NOT_CHARGING }
                        .first()
                }
                var chargingFinishedData: ChargingFinishedData? = null
                navigation.stopCharging { chargingFinishedData = it }
                val afterChargingProgress = afterChargingProgressDeferred.await()
                assertFalse(afterChargingProgress.isChargingExpected)
                assertEquals(RouteProgressState.TRACKING, afterChargingProgress.currentState)
                assertNotNull(chargingFinishedData)
            }
        }

    /**
     * Companion to the tests above, but for charging at an INTERMEDIATE waypoint instead of
     * departure. A custom [ArrivalController] withholds auto-advance past the charging
     * waypoint's leg, since the native FSM only reaches CHARGING while still "at" it - otherwise
     * it reports EXTRA_CHARGING regardless of state of charge.
     */
    @Test
    fun chargingStateAndIsChargingExpectedReflectNativeFsmAtIntermediateWaypoint() =
        sdkTest {
            val evRoute = EvRoutesProvider.getBerlinEvRoute(context, mockWebServerRule.baseUrl)
            mockWebServerRule.requestHandlers.add(evRoute.mockWebServerHandler)

            withMapboxNavigation(historyRecorderRule = mapboxHistoryTestRule) { navigation ->
                val routes = stayOnPosition(evRoute.origin, bearing = 0f) {
                    navigation.startTripSession()
                    navigation.requestRoutes(evRoute.routeOptions)
                        .getSuccessfulResultOrThrowException()
                        .routes
                }
                val route = routes.first()

                // Hold at any leg ending in an EV charging waypoint instead of auto-advancing.
                navigation.setArrivalController(
                    object : ArrivalController {
                        override fun navigateNextRouteLeg(
                            routeLegProgress: RouteLegProgress,
                        ): Boolean {
                            return routeLegProgress.legDestination?.type !in setOf(
                                LegWaypoint.EV_CHARGING_ADDED,
                                LegWaypoint.EV_CHARGING_USER_PROVIDED,
                            )
                        }
                    },
                )
                navigation.setNavigationRoutesAsync(routes)

                // The charging station is a via-waypoint inserted by the router, so its
                // position is only known from the route response.
                val routeWaypoints = requireNotNull(route.waypoints) {
                    "expected the mocked EV route response to report waypoint metadata"
                }
                val chargingStationLocation = requireNotNull(routeWaypoints[1].location()) {
                    "expected the charging waypoint to carry a location"
                }

                // Drive to TRACKING, then pin the vehicle onto the waypoint directly - driving
                // continuously across the leg boundary is unreliable for the map matcher.
                navigation.moveAlongTheRouteUntilTracking(route, mockLocationReplayerRule)
                stayOnPosition(chargingStationLocation, bearing = 0f) {
                    navigation.moveAlongTheCurrentRouteUntilLocation(chargingStationLocation)

                    // Unlike departure, this is a normal leg arrival, so state stays COMPLETE.
                    val awaitChargingProgress = navigation.routeProgressUpdates()
                        .filter { it.chargingState == ChargingState.AWAIT_CHARGING }
                        .first()
                    assertEquals(RouteProgressState.COMPLETE, awaitChargingProgress.currentState)
                    assertTrue(awaitChargingProgress.isChargingExpected)

                    // The arrival controller keeps the vehicle "at" the leg, so this moves to
                    // CHARGING rather than skipping ahead to EXTRA_CHARGING.
                    val chargingProgressDeferred = async {
                        navigation.routeProgressUpdates()
                            .filter { it.chargingState != ChargingState.AWAIT_CHARGING }
                            .first()
                    }
                    navigation.startCharging()
                    val chargingProgress = chargingProgressDeferred.await()

                    assertEquals(ChargingState.CHARGING, chargingProgress.chargingState)
                    assertEquals(RouteProgressState.COMPLETE, chargingProgress.currentState)
                    assertTrue(chargingProgress.isChargingExpected)

                    // Unplugging finishes the leg on the native side, independent of the
                    // withheld Kotlin-side auto-advance.
                    val afterStopProgressDeferred = async {
                        navigation.routeProgressUpdates()
                            .filter { it.chargingState == ChargingState.NOT_CHARGING }
                            .first()
                    }
                    var chargingFinishedData: ChargingFinishedData? = null
                    navigation.stopCharging { chargingFinishedData = it }
                    val afterStopProgress = afterStopProgressDeferred.await()
                    assertEquals(ChargingState.NOT_CHARGING, afterStopProgress.chargingState)
                    assertFalse(afterStopProgress.isChargingExpected)
                    assertNotNull(chargingFinishedData)
                }
            }
        }

    /** Target state of charge (`charge_to`) of the charging station at this route's origin. */
    private fun NavigationRoute.originChargeTo(): Int {
        val originWaypoint = requireNotNull(waypoints?.firstOrNull()) {
            "expected the mocked EV route response to report waypoint metadata"
        }
        return requireNotNull(
            originWaypoint.getUnrecognizedProperty("metadata")
                ?.asJsonObject
                ?.get("charge_to")
                ?.asInt,
        ) {
            "expected the origin waypoint to carry charge_to metadata"
        }
    }
}
