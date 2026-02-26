@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.common.TileDataDomain
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.createTileStore
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.nativeRerouteControllerNoRetryConfig
import com.mapbox.navigation.testing.utils.offline.Tileset
import com.mapbox.navigation.testing.utils.offline.unpackTiles
import com.mapbox.navigation.testing.utils.routes.EvRoutesProvider
import com.mapbox.navigation.testing.utils.routes.MockedEvRoutes
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(Parameterized::class)
class EvOfflineRerouteTest(
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

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.361378213031003
            latitude = 52.49813341962201
        }
    }

    private fun getTestCustomConfig(): String = if (runOptions.nativeReroute) {
        nativeRerouteControllerNoRetryConfig
    } else {
        ""
    }

    /**
     * Verifies that when the user deviates from an online EV route while offline (no internet
     * connection), the SDK falls back to the onboard router for rerouting. Asserts that:
     * - The new route has [RouterOrigin.OFFLINE] origin.
     * - The offline route does not include injected charging station waypoints, since the
     *   onboard router does not support EV waypoint injection.
     */
    @Test
    fun deviateFromOnlinePrimaryRouteWithoutInternet() = sdkTest {
        val originalTestRoute = setupBerlinEvRoute()
        val testRouteAfterReroute = setupBerlinEvRouteAfterReroute()

        withMapboxNavigation(
            tileset = Tileset.Berlin,
        ) { navigation ->
            navigation.startTripSession()
            val requestResult = navigation.requestRoutes(originalTestRoute.routeOptions)
                .getSuccessfulResultOrThrowException()
            assertEquals(RouterOrigin.ONLINE, requestResult.routerOrigin)
            assertEquals(
                "online route for this case is expected to add charging station",
                listOf(3, 3),
                requestResult.routes.map { it.waypoints?.size },
            )
            navigation.setNavigationRoutesAsync(requestResult.routes)
            navigation.moveAlongTheRouteUntilTracking(
                requestResult.routes[0],
                mockLocationReplayerRule,
            )

            withoutInternet {
                stayOnPosition(
                    // off route position
                    latitude = testRouteAfterReroute.origin.latitude(),
                    longitude = testRouteAfterReroute.origin.longitude(),
                    bearing = 280.0f,
                ) {
                    val newRoutes = navigation.routesUpdates()
                        .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
                    assertEquals(RouterOrigin.OFFLINE, newRoutes.navigationRoutes.first().origin)
                    assertEquals(
                        "onboard router doesn't add waypoints",
                        newRoutes.navigationRoutes.map { 2 },
                        newRoutes.navigationRoutes.map { it.waypoints?.size },
                    )
                }
            }
        }
    }

    private fun setupBerlinEvRouteAfterReroute(): MockedEvRoutes {
        val testRouteAfterReroute = EvRoutesProvider.getBerlinEvRouteReroute(
            context,
            mockWebServerRule.baseUrl,
        )
        mockWebServerRule.requestHandlers.add(testRouteAfterReroute.mockWebServerHandler)
        return testRouteAfterReroute
    }

    private fun setupBerlinEvRoute(): MockedEvRoutes {
        val originalTestRoute = EvRoutesProvider.getBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl,
        )
        mockWebServerRule.requestHandlers.add(originalTestRoute.mockWebServerHandler)
        return originalTestRoute
    }

    private suspend inline fun BaseCoreNoCleanUpTest.withMapboxNavigation(
        tileset: Tileset? = null,
        routeRefreshOptions: RouteRefreshOptions? = null,
        block: (MapboxNavigation) -> Unit,
    ) {
        val tilesVersion = tileset?.let { context.unpackTiles(it)[TileDataDomain.NAVIGATION]!! }
        withMapboxNavigation(
            tileStore = createTileStore(),
            tilesVersion = tilesVersion,
            deviceType = DeviceType.HANDHELD,
            historyRecorderRule = mapboxHistoryTestRule,
            customConfig = getTestCustomConfig(),
            routeRefreshOptions = routeRefreshOptions,
            block = block,
        )
    }
}
