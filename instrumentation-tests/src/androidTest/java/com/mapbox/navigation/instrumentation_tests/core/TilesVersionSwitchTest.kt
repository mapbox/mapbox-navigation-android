package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.TileDataDomain
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.NavigationVersionSwitchObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.ui.utils.coroutines.versionSwitchObserver
import com.mapbox.navigation.testing.utils.DelayedResponseModifier
import com.mapbox.navigation.testing.utils.createTileStoreWithFastRetryBackoff
import com.mapbox.navigation.testing.utils.getTestRerouteCustomConfig
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.offline.Tileset
import com.mapbox.navigation.testing.utils.offline.unpackTiles
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the behaviour of an active navigation session across a tiles version switch.
 *
 * Online tiles come from the mock server, which has none, so the navigator falls back to the
 * unpacked tileset once it gives up on resolving the online version.
 */
@OptIn(ExperimentalMapboxNavigationAPI::class)
class TilesVersionSwitchTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    /**
     * Files cleanup is required because the tests expect the navigator to have no tiles for the
     * route before it falls back to the unpacked tileset.
     */
    @get:Rule
    val clearFilesRule = ClearFilesRule()

    override fun setupMockLocation(): Location {
        val origin = RoutesProvider.near_munich_with_waypoints(context).routeWaypoints.first()
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
    }

    /**
     * Verifies that switching to the offline fallback tiles version keeps the active navigation
     * session: the route is not set again, the progress keeps the same route and does not go back
     * to [RouteProgressState.INITIALIZED], and tracking continues.
     */
    @Test
    fun navigation_session_is_kept_when_switching_to_fallback_tiles_version() = sdkTest(120_000) {
        val mockRoute = RoutesProvider.near_munich_with_waypoints(context)
        val originLocation = mockRoute.routeWaypoints.first()
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        val tilesVersion = context.unpackTiles(Tileset.NearMunich)[TileDataDomain.NAVIGATION]!!
        withMapboxNavigation(
            tileStore = createTileStoreWithFastRetryBackoff(),
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestRerouteCustomConfig(),
        ) { navigation ->
            val routes = stayOnPosition(originLocation, bearing = 0.0f) {
                navigation.startTripSession()
                navigation.requestRoutes(
                    RouteOptions.builder()
                        .applyDefaultNavigationOptions()
                        .applyLanguageAndVoiceUnitOptions(context)
                        .baseUrl(mockWebServerRule.baseUrl)
                        .coordinatesList(mockRoute.routeWaypoints)
                        .build(),
                ).getSuccessfulResultOrThrowException().routes
            }

            navigation.setNavigationRoutesAsync(routes)
            navigation.moveAlongTheRouteUntilTracking(
                routes[0],
                mockLocationReplayerRule,
                1,
                endReplay = false,
            )

            // Observers are invoked synchronously, so everything recorded after the flag is set
            // happened after the version switch.
            var switchedToFallback = false
            val progressAfterSwitch = mutableListOf<RouteProgress>()
            val routesUpdatesAfterSwitch = mutableListOf<RoutesUpdatedResult>()
            val versionSwitchObserver = object : NavigationVersionSwitchObserver {
                override fun onSwitchToFallbackVersion(tilesVersion: String?) {
                    switchedToFallback = true
                }

                override fun onSwitchToTargetVersion(tilesVersion: String?) {
                }
            }
            val routeProgressObserver = RouteProgressObserver { progress ->
                if (switchedToFallback) {
                    progressAfterSwitch.add(progress)
                }
            }
            val routesObserver = RoutesObserver { result ->
                if (switchedToFallback) {
                    routesUpdatesAfterSwitch.add(result)
                }
            }
            navigation.registerNavigationVersionSwitchObserver(versionSwitchObserver)
            navigation.registerRouteProgressObserver(routeProgressObserver)
            navigation.registerRoutesObserver(routesObserver)

            withoutInternet {
                navigation.versionSwitchObserver().first { it == tilesVersion }
                withTimeout(30.seconds) {
                    navigation.routeProgressUpdates()
                        .filter { it.currentState == RouteProgressState.TRACKING }
                        .take(5)
                        .toList()
                }
                // map matching keeps working on the fallback tiles version
                withTimeout(30.seconds) {
                    navigation.flowLocationMatcherResult()
                        .scan(0) { notDegradedInRow, location ->
                            if (location.isDegradedMapMatching) 0 else notDegradedInRow + 1
                        }
                        .first { it == 3 }
                }
            }

            navigation.unregisterNavigationVersionSwitchObserver(versionSwitchObserver)
            navigation.unregisterRouteProgressObserver(routeProgressObserver)
            navigation.unregisterRoutesObserver(routesObserver)

            assertTrue(
                "routes were set again after the switch: " +
                    routesUpdatesAfterSwitch.map { it.reason },
                routesUpdatesAfterSwitch.isEmpty(),
            )
            assertTrue(
                "progress went back to INITIALIZED after the switch",
                progressAfterSwitch.none { it.currentState == RouteProgressState.INITIALIZED },
            )
            assertTrue(
                "progress switched to another route after the switch",
                progressAfterSwitch.all { it.navigationRoute.id == routes[0].id },
            )
            assertEquals(routes[0].id, navigation.getNavigationRoutes().first().id)
        }
    }

    /**
     * Verifies that an online route request which is in flight while the navigator switches to
     * the fallback tiles version completes successfully instead of being aborted by the switch.
     *
     * The mock tile server has no tiles versions, so the navigator falls back to the unpacked
     * tileset shortly after it is created. The route response is held until that moment.
     */
    @Test
    fun route_request_in_flight_completes_across_tiles_version_switch() = sdkTest(120_000) {
        val mockRoute = RoutesProvider.near_munich_with_waypoints(context)
        val originLocation = mockRoute.routeWaypoints.first()
        val requestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = mockRoute.routeResponseJson,
            expectedCoordinates = mockRoute.routeWaypoints,
            relaxedExpectedCoordinates = false,
        )
        // the response is held until the fallback switch, see below
        val responseDelay = DelayedResponseModifier(RESPONSE_HOLD_MILLIS)
        requestHandler.jsonResponseModifier = responseDelay
        mockWebServerRule.requestHandlers.add(requestHandler)
        val tilesVersion = context.unpackTiles(Tileset.NearMunich)[TileDataDomain.NAVIGATION]!!
        withMapboxNavigation(
            tileStore = createTileStoreWithFastRetryBackoff(),
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestRerouteCustomConfig(),
        ) { navigation ->
            val fallbackSwitch = async(start = CoroutineStart.UNDISPATCHED) {
                navigation.versionSwitchObserver().first { it == tilesVersion }
                // the switch happened while the request is in flight: let the response through
                responseDelay.interruptDelay()
            }

            val routes = stayOnPosition(originLocation, bearing = 0.0f) {
                navigation.startTripSession()
                navigation.requestRoutes(
                    RouteOptions.builder()
                        .applyDefaultNavigationOptions()
                        .applyLanguageAndVoiceUnitOptions(context)
                        .baseUrl(mockWebServerRule.baseUrl)
                        .coordinatesList(mockRoute.routeWaypoints)
                        .build(),
                ).getSuccessfulResultOrThrowException().routes
            }

            assertTrue(
                "tiles version did not switch while the request was in flight",
                fallbackSwitch.isCompleted,
            )
            assertEquals(RouterOrigin.ONLINE, routes.first().origin)
            navigation.setNavigationRoutesAsync(routes)
            assertEquals(routes.first().id, navigation.getNavigationRoutes().first().id)
        }
    }

    private companion object {
        // long enough to never expire on its own: the test ends the hold itself
        const val RESPONSE_HOLD_MILLIS = 60_000L
    }
}
