package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.routes.EvRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockedEvRoutes
import com.mapbox.navigation.instrumentation_tests.utils.routes.getChargingStationIds
import com.mapbox.navigation.instrumentation_tests.utils.routes.getChargingStationPowerCurrentTypes
import com.mapbox.navigation.instrumentation_tests.utils.routes.getChargingStationPowersKW
import com.mapbox.navigation.instrumentation_tests.utils.routes.getChargingStationTypes
import com.mapbox.navigation.instrumentation_tests.utils.tiles.OfflineRegions
import com.mapbox.navigation.instrumentation_tests.utils.tiles.withMapboxNavigationAndOfflineTilesForRegion
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.instrumentation_tests.utils.withoutInternet
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.NavigationRouteAlternativesResult
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.alternativesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// TODO: remove in the scope of NAVAND-1351
const val INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE = 80_000L

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
    fun deviateFromOnlinePrimaryRouteWithoutInternetAndGetBackOnline() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
    ) {
        val originalTestRoute = setupBerlinEvRoute()
        val testRouteAfterReroute = setupBerlinEvRouteAfterReroute()

        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            navigation.registerRouteAlternativesObserver(
                AdvancedAlternativesObserverFromDocumentation(navigation)
            )
            navigation.startTripSession()
            val requestResult = navigation.requestRoutes(originalTestRoute.routeOptions)
                .getSuccessfulResultOrThrowException()
            assertEquals(RouterOrigin.Offboard, requestResult.routerOrigin)
            assertEquals(
                "online route for this case is expected to add charging station",
                listOf(3, 3),
                requestResult.routes.map { it.waypoints?.size }
            )
            val initialOnlineRoutes = requestResult.routes
            navigation.setNavigationRoutesAsync(initialOnlineRoutes)

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
                        "Fallback to offline navigation doesn't change a trip plan",
                        initialOnlineRoutes.first().waypoints?.map { it.location() }?.drop(1),
                        newRoutes.navigationRoutes.first().waypoints?.map { it.location() }?.drop(1)
                    )
                    assertEquals(
                        "Fallback to offline navigation doesn't change a trip plan",
                        initialOnlineRoutes.first().getChargingStationIds(),
                        newRoutes.navigationRoutes.first().getChargingStationIds()
                    )
                    assertEquals(
                        "Fallback to offline navigation doesn't change charging stations power",
                        initialOnlineRoutes.first().getChargingStationPowersKW(),
                        newRoutes.navigationRoutes.first().getChargingStationPowersKW()
                    )
                    assertEquals(
                        "Fallback to offline navigation doesn't change charging station current type",
                        initialOnlineRoutes.first().getChargingStationPowerCurrentTypes(),
                        newRoutes.navigationRoutes.first().getChargingStationPowerCurrentTypes()
                    )
                    assertEquals(
                        "Fallback to offline navigation doesn't change charging station type",
                        initialOnlineRoutes.first().getChargingStationTypes(),
                        newRoutes.navigationRoutes.first().getChargingStationTypes()
                    )
                }
            }
            stayOnPosition(
                // off route position
                latitude = testRouteAfterReroute.origin.latitude(),
                longitude = testRouteAfterReroute.origin.longitude(),
                bearing = 280.0f
            ) {
                val onlineRoutesUpdate = navigation.routesUpdates()
                    .first { it.navigationRoutes.first().origin == RouterOrigin.Offboard }
                val onlineRoute = onlineRoutesUpdate.navigationRoutes.first()
                assertEquals(
                    "Switch back to an online route doesn't change a trip plan",
                    initialOnlineRoutes.first().waypoints?.map { it.location() }?.drop(1),
                    onlineRoute.waypoints?.map { it.location() }?.drop(1)
                )
                assertEquals(
                    "Switch back to an online route doesn't change a trip plan",
                    initialOnlineRoutes.first().getChargingStationIds(),
                    onlineRoute.getChargingStationIds()
                )
                assertEquals(
                    "Switch back to an online route doesn't change charging stations power",
                    initialOnlineRoutes.first().getChargingStationPowersKW(),
                    onlineRoute.getChargingStationPowersKW()
                )
                assertEquals(
                    "Switch back to an online route doesn't change charging station current type",
                    initialOnlineRoutes.first().getChargingStationPowerCurrentTypes(),
                    onlineRoute.getChargingStationPowerCurrentTypes()
                )
                assertEquals(
                    "Switch back to an online route doesn't change charging station type",
                    initialOnlineRoutes.first().getChargingStationTypes(),
                    onlineRoute.getChargingStationTypes()
                )
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
                .uuid("route-similar-to-$primaryRouteResponseUUID")
                .build()
                .toJson(),
            expectedCoordinates = offlineRoutes.first().routeOptions.coordinatesList(),
        )
        mockWebServerRule.requestHandlers.add(evRouteRequestHandler)
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
