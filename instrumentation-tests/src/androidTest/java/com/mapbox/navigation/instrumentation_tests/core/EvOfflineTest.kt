@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.common.TileDataDomain
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.instrumentation_tests.utils.assumeNotNROBecauseOfClientSideUpdate
import com.mapbox.navigation.instrumentation_tests.utils.assumeNotNROBecauseToBuilderIsRequiredForTest
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.refreshStates
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.createTileStore
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.offline.Tileset
import com.mapbox.navigation.testing.utils.offline.unpackTiles
import com.mapbox.navigation.testing.utils.routes.EvRoutesProvider
import com.mapbox.navigation.testing.utils.routes.MockedEvRouteWithSingleUserProvidedChargingStation
import com.mapbox.navigation.testing.utils.routes.MockedEvRoutes
import com.mapbox.navigation.testing.utils.setTestRouteRefreshInterval
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMapboxNavigationAPI::class)
class EvOfflineTest : BaseCoreNoCleanUpTest() {

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

    @Test
    fun requestRouteWithoutInternetAndTiles() = sdkTest {
        val testRoute = setupBerlinEvRoute()
        withMapboxNavigation { navigation ->
            withoutInternet {
                val routes = navigation.requestRoutes(testRoute.routeOptions)
                assertTrue(routes is RouteRequestResult.Failure)
            }
        }
    }

    @Test
    fun startNavigationOfflineThenSwitchToOnlineRouteWhenInternetAppears() = sdkTest {
        val originalTestRoute = setupBerlinEvRoute()

        withMapboxNavigation(
            tileset = Tileset.Berlin,
        ) { navigation ->
            navigation.startTripSession()
            stayOnPosition(
                originalTestRoute.origin.latitude(),
                originalTestRoute.origin.longitude(),
                0.0f,
            ) {
                withoutInternet {
                    navigation.waitUntilNNIsReadyToBuildOfflineRoutesAfterColdStart()
                    val requestResult = navigation.requestRoutes(originalTestRoute.routeOptions)
                        .getSuccessfulResultOrThrowException()
                    assertEquals(RouterOrigin.OFFLINE, requestResult.routerOrigin)
                    navigation.setNavigationRoutesAsync(requestResult.routes)

                    assertEquals(
                        "onboard router doesn't add charging waypoints",
                        listOf(2, 2),
                        requestResult.routes.map { it.waypoints?.size },
                    )
                }

                val onlineRoutes = navigation.routesUpdates().first {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                        it.navigationRoutes.first().origin == RouterOrigin.ONLINE
                }

                assertEquals(2, onlineRoutes.navigationRoutes.size)
                assertEquals(
                    "online result should have charging station waypoint",
                    listOf(3, 3),
                    onlineRoutes.navigationRoutes.map { it.waypoints?.size },
                )
            }
        }
    }

    @Test
    fun offlineOnlineSwitchWhenOnlineRouteIsTheSameAsCurrentOffline() = sdkTest {
        assumeNotNROBecauseToBuilderIsRequiredForTest()
        val evBerlinTestRoute = EvRoutesProvider.getBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl,
        )
        withMapboxNavigation(
            tileset = Tileset.Berlin,
        ) { navigation ->
            navigation.startTripSession()
            stayOnPosition(
                evBerlinTestRoute.origin.latitude(),
                evBerlinTestRoute.origin.longitude(),
                0.0f,
            ) {
                withoutInternet {
                    navigation.waitUntilNNIsReadyToBuildOfflineRoutesAfterColdStart()
                    val requestResult = navigation.requestRoutes(evBerlinTestRoute.routeOptions)
                        .getSuccessfulResultOrThrowException()
                    assertEquals(RouterOrigin.OFFLINE, requestResult.routerOrigin)
                    navigation.setNavigationRoutesAsync(requestResult.routes)

                    assertEquals(
                        "onboard router doesn't add charging waypoints",
                        listOf(2, 2),
                        requestResult.routes.map { it.waypoints?.size },
                    )
                    val offlineRoutes = requestResult.routes
                    setupTheSameOnlineRoute(offlineRoutes)
                }
                val onlineRoutes = navigation.routesUpdates().first {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                        it.navigationRoutes.first().origin == RouterOrigin.ONLINE
                }
                assertEquals(2, onlineRoutes.navigationRoutes.size)
            }
        }
    }

    @Test
    fun startOfflineWithUserProvidedChargingStationsThenSwitchToOnlineRouteWhenInternetAppears() =
        sdkTest {
            val testRoute = setupBerlinEvRouteWithCustomProvidedChargingStation()

            withMapboxNavigation(
                tileset = Tileset.Berlin,
            ) { navigation ->
                navigation.startTripSession()
                stayOnPosition(
                    testRoute.origin.latitude(),
                    testRoute.origin.longitude(),
                    testRoute.originBearing,
                ) {
                    withoutInternet {
                        navigation.waitUntilNNIsReadyToBuildOfflineRoutesAfterColdStart()
                        val requestResult = navigation.requestRoutes(testRoute.routeOptions)
                            .getSuccessfulResultOrThrowException()
                        assertEquals(RouterOrigin.OFFLINE, requestResult.routerOrigin)
                        navigation.setNavigationRoutesAsync(requestResult.routes)

                        val offlinePrimaryRoute = requestResult.routes.first()
                        verifyUserProvidedChargingStationMetadata(offlinePrimaryRoute, testRoute)
                    }

                    val onlineRoutes = navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                            it.navigationRoutes.first().origin == RouterOrigin.ONLINE
                    }
                    val onlinePrimaryRoute = onlineRoutes.navigationRoutes.first()

                    verifyUserProvidedChargingStationMetadata(onlinePrimaryRoute, testRoute)
                }
            }
        }

    @Test
    fun refresh_online_ev_route_offline() = sdkTest {
        assumeNotNROBecauseOfClientSideUpdate()

        val originalTestRoute = setupBerlinEvRoute()

        val routeRefreshOptions = RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
            .build()
        routeRefreshOptions.setTestRouteRefreshInterval(1_500L)

        withMapboxNavigation(
            tileset = Tileset.Berlin,
            routeRefreshOptions = routeRefreshOptions,
        ) { navigation ->
            stayOnPosition(
                latitude = originalTestRoute.origin.latitude(),
                longitude = originalTestRoute.origin.longitude(),
                bearing = 280.0f,
            ) {
                navigation.startTripSession()
                val onlineResult = navigation.requestRoutes(originalTestRoute.routeOptions)
                    .getSuccessfulResultOrThrowException()
                assertEquals(RouterOrigin.ONLINE, onlineResult.routerOrigin)
                assertEquals(
                    "online route for this case is expected to add charging station",
                    listOf(3, 3),
                    onlineResult.routes.map { it.waypoints?.size },
                )
                navigation.setNavigationRoutesAsync(onlineResult.routes)

                withoutInternet {
                    navigation.refreshStates().first {
                        it.state == RouteRefreshExtra.REFRESH_STATE_CLEARED_EXPIRED
                    }
                    val refreshedInOfflineResult = navigation.routesUpdates()
                        .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
                    assertEquals(
                        RouterOrigin.ONLINE,
                        refreshedInOfflineResult.navigationRoutes.first().origin,
                    )
                    assertEquals(
                        "waypoints have been updated after failed refresh",
                        onlineResult.routes.map { it.waypoints },
                        refreshedInOfflineResult.navigationRoutes.map { it.waypoints },
                    )
                    assertEquals(
                        "SOC annotations have been changed during failed route refresh",
                        onlineResult.routes.map {
                            it.directionsRoute.legs()?.map {
                                it.annotation()
                                    ?.getUnrecognizedProperty("state_of_charge")
                            }
                        },
                        refreshedInOfflineResult.navigationRoutes.map {
                            it.directionsRoute.legs()?.map {
                                it.annotation()
                                    ?.getUnrecognizedProperty("state_of_charge")
                            }
                        },
                    )
                }
            }
        }
    }

    private fun setupBerlinEvRoute(): MockedEvRoutes {
        val originalTestRoute = EvRoutesProvider.getBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl,
        )
        mockWebServerRule.requestHandlers.add(originalTestRoute.mockWebServerHandler)
        return originalTestRoute
    }

    private fun setupTheSameOnlineRoute(offlineRoutes: List<NavigationRoute>) {
        val primaryRoute = offlineRoutes.first()
        val primaryRouteResponseUUID = primaryRoute.id.substring(
            0,
            primaryRoute.id.indexOf("#") - 1,
        )
        val evRouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = DirectionsResponse.builder()
                .routes(offlineRoutes.map { it.directionsRoute })
                .uuid("route-similar-to-$primaryRouteResponseUUID")
                .code("Ok")
                .build()
                .toJson(),
            expectedCoordinates = primaryRoute.routeOptions.coordinatesList(),
        )
        mockWebServerRule.requestHandlers.add(evRouteRequestHandler)
    }

    private fun setupBerlinEvRouteWithCustomProvidedChargingStation():
        MockedEvRouteWithSingleUserProvidedChargingStation {
        val testRoute = EvRoutesProvider.getBerlinEvRouteWithUserProvidedChargingStation(
            context,
            // Pass null to use a real server
            mockWebServerRule.baseUrl,
        )
        mockWebServerRule.requestHandlers.add(testRoute.mockWebServerHandler)
        return testRoute
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
            deviceType = DeviceType.AUTOMOBILE,
            historyRecorderRule = mapboxHistoryTestRule,
            routeRefreshOptions = routeRefreshOptions,
            block = block,
        )
    }
}

private fun NavigationRoute.getChargingStationsType() = getWaypointMetadata("type")
private fun NavigationRoute.getChargingStationsPowerKw() = getWaypointMetadataInt("power_kw")
private fun NavigationRoute.getChargingStationsCurrentType() = getWaypointMetadata("current_type")
private fun NavigationRoute.getChargingStationsId() = getWaypointMetadata("station_id")

private fun NavigationRoute.getWaypointMetadata(name: String): List<String?> {
    return waypoints?.map {
        it.unrecognizedJsonProperties?.get("metadata")?.asJsonObject?.get(name)?.asString
    } ?: emptyList()
}

private fun NavigationRoute.getWaypointMetadataInt(name: String): List<Int?> {
    return waypoints?.map {
        it.unrecognizedJsonProperties?.get("metadata")?.asJsonObject?.get(name)?.asInt
    } ?: emptyList()
}

private fun verifyUserProvidedChargingStationMetadata(
    route: NavigationRoute,
    testRoute: MockedEvRouteWithSingleUserProvidedChargingStation,
) {
    assertEquals(
        listOf(null, "user-provided-charging-station", null),
        route.getChargingStationsType(),
    )
    assertEquals(
        listOf(null, testRoute.chargingStationPowerKw, null),
        route.getChargingStationsPowerKw(),
    )
    assertEquals(
        listOf(null, testRoute.chargingStationId, null),
        route.getChargingStationsId(),
    )
    assertEquals(
        listOf(null, testRoute.currentType, null),
        route.getChargingStationsCurrentType(),
    )
}

// workaround for https://mapbox.atlassian.net/browse/NN-3386
private suspend fun MapboxNavigation.waitUntilNNIsReadyToBuildOfflineRoutesAfterColdStart() {
    flowLocationMatcherResult().first { !it.isDegradedMapMatching }
}
