@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.core.routerefresh.RouteRefreshExtra
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.routes.EvRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockedEvRouteWithSingleUserProvidedChargingStation
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockedEvRoutes
import com.mapbox.navigation.instrumentation_tests.utils.tiles.OfflineRegions
import com.mapbox.navigation.instrumentation_tests.utils.tiles.TIME_TO_LOAD_TILES
import com.mapbox.navigation.instrumentation_tests.utils.tiles.withMapboxNavigationAndOfflineTilesForRegion
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.instrumentation_tests.utils.withoutInternet
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.DEFAULT_TIMEOUT_FOR_SDK_TEST
import com.mapbox.navigation.testing.ui.utils.coroutines.NavigationRouteAlternativesResult
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.alternativesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.refreshStates
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

// TODO: remove in the scope of NAVAND-1351
const val INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE = DEFAULT_TIMEOUT_FOR_SDK_TEST +
    TIME_TO_LOAD_TILES

class EvOfflineTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.361378213031003
            latitude = 52.49813341962201
        }
    }

    @Test
    fun requestRouteWithoutInternetAndTiles() = sdkTest {
        val testRoute = setupBerlinEvRoute()
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            withoutInternet {
                val routes = navigation.requestRoutes(testRoute.routeOptions)
                assertTrue(routes is RouteRequestResult.Failure)
            }
        }
    }

    @Ignore("https://mapbox.atlassian.net/browse/NAVAND-2557")
    @Test
    fun startNavigationOfflineThenSwitchToOnlineRouteWhenInternetAppears() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
    ) {
        val originalTestRoute = setupBerlinEvRoute()
        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            navigation.registerRouteAlternativesObserver(
                AdvancedAlternativesObserverFromDocumentation(navigation)
            )
            navigation.startTripSession()
            stayOnPosition(
                originalTestRoute.origin.latitude(),
                originalTestRoute.origin.longitude(),
                0.0f,
            ) {
                withoutInternet {
                    val requestResult = navigation.requestRoutes(originalTestRoute.routeOptions)
                        .getSuccessfulResultOrThrowException()
                    assertEquals(RouterOrigin.Onboard, requestResult.routerOrigin)
                    navigation.setNavigationRoutesAsync(requestResult.routes)

                    assertEquals(
                        "onboard router doesn't add charging waypoints",
                        listOf(2, 2),
                        requestResult.routes.map { it.waypoints?.size }
                    )
                }
                val onlineRoutes = navigation.routesUpdates().first {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                        it.navigationRoutes.first().origin == RouterOrigin.Offboard
                }
                assertEquals(2, onlineRoutes.navigationRoutes.size)
                assertEquals(
                    "online result should have charging station waypoint",
                    listOf(3, 3),
                    onlineRoutes.navigationRoutes.map { it.waypoints?.size }
                )
            }
        }
    }

    @Ignore("https://mapbox.atlassian.net/browse/NAVAND-2557")
    @Test
    fun offlineOnlineSwitchWhenOnlineRouteIsTheSameAsCurrentOffline() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
    ) {
        val evBerlinTestRoute = EvRoutesProvider.getBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl
        )
        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            navigation.registerRouteAlternativesObserver(
                AdvancedAlternativesObserverFromDocumentation(navigation)
            )
            navigation.startTripSession()
            stayOnPosition(
                evBerlinTestRoute.origin.latitude(),
                evBerlinTestRoute.origin.longitude(),
                0.0f,
            ) {
                withoutInternet {
                    val requestResult = navigation.requestRoutes(evBerlinTestRoute.routeOptions)
                        .getSuccessfulResultOrThrowException()
                    assertEquals(RouterOrigin.Onboard, requestResult.routerOrigin)
                    navigation.setNavigationRoutesAsync(requestResult.routes)

                    assertEquals(
                        "onboard router doesn't add charging waypoints",
                        listOf(2, 2),
                        requestResult.routes.map { it.waypoints?.size }
                    )
                    val offlineRoutes = requestResult.routes
                    setupTheSameOnlineRoute(offlineRoutes)
                }
                val onlineRoutes = navigation.routesUpdates().first {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                        it.navigationRoutes.first().origin == RouterOrigin.Offboard
                }
                assertEquals(2, onlineRoutes.navigationRoutes.size)
            }
        }
    }

    @Ignore("https://mapbox.atlassian.net/browse/NAVAND-2557")
    @Test
    fun startOfflineWithUserProvidedChargingStationsThenSwitchToOnlineRouteWhenInternetAppears() =
        sdkTest(
            timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
        ) {
            val testRoute = setupBerlinEvRouteWithCustomProvidedChargingStation()
            withMapboxNavigationAndOfflineTilesForRegion(
                OfflineRegions.Berlin,
                historyRecorderRule = mapboxHistoryTestRule
            ) { navigation ->
                navigation.registerRouteAlternativesObserver(
                    AdvancedAlternativesObserverFromDocumentation(navigation)
                )
                navigation.startTripSession()
                stayOnPosition(
                    testRoute.origin.latitude(),
                    testRoute.origin.longitude(),
                    testRoute.originBearing,
                ) {
                    withoutInternet {
                        val requestResult = navigation.requestRoutes(testRoute.routeOptions)
                            .getSuccessfulResultOrThrowException()
                        assertEquals(RouterOrigin.Onboard, requestResult.routerOrigin)
                        navigation.setNavigationRoutesAsync(requestResult.routes)
                        val offlinePrimaryRoute = requestResult.routes.first()
                        verifyUserProvidedChargingStationMetadata(offlinePrimaryRoute, testRoute)
                    }
                    val onlineRoutes = navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                            it.navigationRoutes.first().origin == RouterOrigin.Offboard
                    }
                    val onlinePrimaryRoute = onlineRoutes.navigationRoutes.first()

                    verifyUserProvidedChargingStationMetadata(onlinePrimaryRoute, testRoute)
                }
            }
        }

    @Ignore("https://mapbox.atlassian.net/browse/NAVAND-2557")
    @Test
    fun offlineOnlineSwitchWhenOnlineRouteIsTheSameAsCurrentOfflineWithSimpleObserver() =
        sdkTest(
            timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
        ) {
            val evBerlinTestRoute = EvRoutesProvider.getBerlinEvRoute(
                context,
                mockWebServerRule.baseUrl
            )
            withMapboxNavigationAndOfflineTilesForRegion(
                OfflineRegions.Berlin,
                historyRecorderRule = mapboxHistoryTestRule
            ) { navigation ->
                val firstAlternativesUpdatedCallbackWithOnlineRoutesDeferred = async {
                    navigation.alternativesUpdates()
                        .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
                        .first {
                            it.routerOrigin == RouterOrigin.Offboard
                        }
                }
                navigation.registerRouteAlternativesObserver(
                    SimpleAlternativesObserverFromDocumentation(navigation)
                )

                navigation.startTripSession()
                stayOnPosition(
                    evBerlinTestRoute.origin.latitude(),
                    evBerlinTestRoute.origin.longitude(),
                    0.0f,
                ) {
                    withoutInternet {
                        val requestResult = navigation.requestRoutes(evBerlinTestRoute.routeOptions)
                            .getSuccessfulResultOrThrowException()
                        assertEquals(RouterOrigin.Onboard, requestResult.routerOrigin)
                        navigation.setNavigationRoutesAsync(requestResult.routes)

                        assertEquals(
                            "onboard router doesn't add charging waypoints",
                            listOf(2, 2),
                            requestResult.routes.map { it.waypoints?.size }
                        )
                        val offlineRoutes = requestResult.routes
                        setupTheSameOnlineRoute(offlineRoutes)
                    }

                    val firstCallback = firstAlternativesUpdatedCallbackWithOnlineRoutesDeferred
                        .await()
                    val firstRoute = firstCallback.alternatives.first()
                    assertNull(
                        "First alternatives in this case doesn't have " +
                            "deviation point from primary route",
                        navigation.getAlternativeMetadataFor(firstRoute)
                    )

                    val onlineRoutes = navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE &&
                            it.navigationRoutes.any { it.origin == RouterOrigin.Offboard }
                    }
                    assertEquals(2, onlineRoutes.navigationRoutes.size)
                    assertEquals(
                        "online alternative that is the same as current primary " +
                            "route is ignored with simple alternatives observer implementation",
                        1,
                        onlineRoutes.ignoredRoutes.size
                    )
                }
            }
        }

    @Test
    fun deviateFromOnlinePrimaryRouteWithoutInternet() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
    ) {
        val originalTestRoute = setupBerlinEvRoute()
        val testRouteAfterReroute = setupBerlinEvRouteAfterReroute()

        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            navigation.startTripSession()
            val requestResult = navigation.requestRoutes(originalTestRoute.routeOptions)
                .getSuccessfulResultOrThrowException()
            assertEquals(RouterOrigin.Offboard, requestResult.routerOrigin)
            assertEquals(
                "online route for this case is expected to add charging station",
                listOf(3, 3),
                requestResult.routes.map { it.waypoints?.size }
            )
            navigation.setNavigationRoutesAsync(requestResult.routes)

            withoutInternet {
                stayOnPosition(
                    // off route position
                    latitude = testRouteAfterReroute.origin.latitude(),
                    longitude = testRouteAfterReroute.origin.longitude(),
                    bearing = 280.0f
                ) {
                    val newRoutes = navigation.routesUpdates()
                        .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
                    assertEquals(RouterOrigin.Onboard, newRoutes.navigationRoutes.first().origin)
                    assertEquals(
                        "onboard router doesn't add waypoints",
                        newRoutes.navigationRoutes.map { 2 },
                        newRoutes.navigationRoutes.map { it.waypoints?.size }
                    )
                }
            }
        }
    }

    @Test
    fun refresh_online_ev_route_offline() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
    ) {
        val originalTestRoute = setupBerlinEvRoute()

        val routeRefreshOptions = RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
            .build()
        RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
            isAccessible = true
            set(routeRefreshOptions, 1_500L)
        }

        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
            historyRecorderRule = mapboxHistoryTestRule,
            routeRefreshOptions = routeRefreshOptions
        ) { navigation ->
            stayOnPosition(
                latitude = originalTestRoute.origin.latitude(),
                longitude = originalTestRoute.origin.longitude(),
                bearing = 280.0f
            ) {
                navigation.startTripSession()
                val onlineResult = navigation.requestRoutes(originalTestRoute.routeOptions)
                    .getSuccessfulResultOrThrowException()
                assertEquals(RouterOrigin.Offboard, onlineResult.routerOrigin)
                assertEquals(
                    "online route for this case is expected to add charging station",
                    listOf(3, 3),
                    onlineResult.routes.map { it.waypoints?.size }
                )
                navigation.setNavigationRoutesAsync(onlineResult.routes)

                withoutInternet {
                    navigation.refreshStates().first {
                        it.state == RouteRefreshExtra.REFRESH_STATE_CLEARED_EXPIRED
                    }
                    val refreshedInOfflineResult = navigation.routesUpdates()
                        .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
                    assertEquals(
                        RouterOrigin.Offboard,
                        refreshedInOfflineResult.navigationRoutes.first().origin
                    )
                    assertEquals(
                        "waypoints have been updated after failed refresh",
                        onlineResult.routes.map { it.waypoints },
                        refreshedInOfflineResult.navigationRoutes.map { it.waypoints }
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
                        }
                    )
                }
            }
        }
    }

    private fun setupBerlinEvRouteAfterReroute(): MockedEvRoutes {
        val testRouteAfterReroute = EvRoutesProvider.getBerlinEvRouteReroute(
            context,
            mockWebServerRule.baseUrl
        )
        mockWebServerRule.requestHandlers.add(testRouteAfterReroute.mockWebServerHandler)
        return testRouteAfterReroute
    }

    private fun setupBerlinEvRoute(): MockedEvRoutes {
        val originalTestRoute = EvRoutesProvider.getBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl
        )
        mockWebServerRule.requestHandlers.add(originalTestRoute.mockWebServerHandler)
        return originalTestRoute
    }

    private fun setupTheSameOnlineRoute(offlineRoutes: List<NavigationRoute>) {
        val primaryRoute = offlineRoutes.first()
        val primaryRouteResponseUUID = primaryRoute.id.substring(
            0,
            primaryRoute.id.indexOf("#") - 1
        )
        val evRouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = offlineRoutes.first().directionsResponse
                .toBuilder()
                .routes(offlineRoutes.map { it.directionsRoute })
                .uuid("route-similar-to-$primaryRouteResponseUUID")
                .build()
                .toJson(),
            expectedCoordinates = offlineRoutes.first().routeOptions.coordinatesList(),
        )
        mockWebServerRule.requestHandlers.add(evRouteRequestHandler)
    }

    private fun setupBerlinEvRouteWithCustomProvidedChargingStation():
        MockedEvRouteWithSingleUserProvidedChargingStation {
        val testRoute = EvRoutesProvider.getBerlinEvRouteWithUserProvidedChargingStation(
            context,
            // Pass null to use a real server
            mockWebServerRule.baseUrl
        )
        mockWebServerRule.requestHandlers.add(testRoute.mockWebServerHandler)
        return testRoute
    }
}

/**
 * Alternatives route observer that is implemented according to our documentation.
 * It's an advanced one because it supports offline-online switching
 */
class AdvancedAlternativesObserverFromDocumentation(
    private val mapboxNavigation: MapboxNavigation
) : NavigationRouteAlternativesObserver {
    override fun onRouteAlternatives(
        routeProgress: RouteProgress,
        alternatives: List<NavigationRoute>,
        routerOrigin: RouterOrigin
    ) {
        val primaryRoute = routeProgress.navigationRoute
        val isPrimaryRouteOffboard = primaryRoute.origin == RouterOrigin.Offboard
        val offboardAlternatives = alternatives.filter { it.origin == RouterOrigin.Offboard }

        when {
            isPrimaryRouteOffboard -> {
                // if the current route is offboard, keep it
                // but consider accepting additional offboard alternatives only and ignore onboard ones
                val updatedRoutes = mutableListOf<NavigationRoute>()
                updatedRoutes.add(primaryRoute)
                updatedRoutes.addAll(offboardAlternatives)
                mapboxNavigation.setNavigationRoutes(updatedRoutes)
            }
            isPrimaryRouteOffboard.not() && offboardAlternatives.isNotEmpty() -> {
                // if the current route is onboard, and there's an offboard route available
                // consider notifying the user that a more accurate route is available and whether they'd want to switch
                // or force the switch like presented
                mapboxNavigation.setNavigationRoutes(offboardAlternatives)
            }
            else -> {
                // in other cases, when current route is onboard and there are no offboard alternatives,
                // just append the new alternatives
                val updatedRoutes = mutableListOf<NavigationRoute>()
                updatedRoutes.add(primaryRoute)
                updatedRoutes.addAll(alternatives)
                mapboxNavigation.setNavigationRoutes(updatedRoutes)
            }
        }
    }

    override fun onRouteAlternativesError(error: RouteAlternativesError) {
        Log.e("AdvancedAlternativesObserverFromDocumentation", "error: $error", error.throwable)
    }
}

/**
 * Simple alternatives route observer that is implemented according to our documentation
 */
class SimpleAlternativesObserverFromDocumentation(
    private val mapboxNavigation: MapboxNavigation
) : NavigationRouteAlternativesObserver {
    override fun onRouteAlternatives(
        routeProgress: RouteProgress,
        alternatives: List<NavigationRoute>,
        routerOrigin: RouterOrigin
    ) {
        val newRoutes = mutableListOf<NavigationRoute>().apply {
            add(mapboxNavigation.getNavigationRoutes().first())
            addAll(alternatives)
        }

        mapboxNavigation.setNavigationRoutes(newRoutes)
    }

    override fun onRouteAlternativesError(error: RouteAlternativesError) {
        Log.e("SimpleAlternativesObserverFromDocumentation", "error: $error", error.throwable)
    }
}

private fun NavigationRoute.getChargingStationsType() = getWaypointMetadata("type")
private fun NavigationRoute.getChargingStationsPowerKw() = getWaypointMetadata("power_kw")
private fun NavigationRoute.getChargingStationsCurrentType() = getWaypointMetadata("current_type")
private fun NavigationRoute.getChargingStationsId() = getWaypointMetadata("station_id")

private fun NavigationRoute.getWaypointMetadata(name: String): List<String?> {
    return waypoints?.map {
        it.unrecognizedJsonProperties?.get("metadata")?.asJsonObject?.get(name)?.asString
    } ?: emptyList()
}

private fun verifyUserProvidedChargingStationMetadata(
    route: NavigationRoute,
    testRoute: MockedEvRouteWithSingleUserProvidedChargingStation
) {
    assertEquals(
        listOf(null, "user-provided-charging-station", null),
        route.getChargingStationsType()
    )
    assertEquals(
        listOf(null, "${testRoute.chargingStationPowerKw}", null),
        route.getChargingStationsPowerKw()
    )
    assertEquals(
        listOf(null, testRoute.chargingStationId, null),
        route.getChargingStationsId()
    )
    assertEquals(
        listOf(null, testRoute.currentType, null),
        route.getChargingStationsCurrentType()
    )
}
