package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
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
    fun startNavigationOfflineWithUserProvidedChargingStationsThenSwitchToOnlineRouteWhenInternetAppears() =
        sdkTest(
            timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
        ) {
            val originBearing = 290.0f
            val chargingStationId = "ocm-54453"
            val chargingStationCurrentType = "dc"
            val chargingStationPower = 50_000
            val chargingStationPowerKw = chargingStationPower / 1000
            val chargingStationLocation = Point.fromLngLat(13.361342, 52.498064)
            val routeOptions = RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(13.361378213031003, 52.49813341962201),
                        chargingStationLocation,
                        Point.fromLngLat(13.393450988895268, 52.50913924804004)
                    )
                )
                .bearingsList(
                    listOf(
                        null,
                        Bearing.builder()
                            .degrees(45.0)
                            .angle(originBearing.toDouble())
                            .build(),
                        null
                    )
                )
                .annotations("state_of_charge")
                .alternatives(true)
                .waypointsPerRoute(true)
                .unrecognizedProperties(
                    mapOf(
                        "engine" to "electric",
                        "ev_initial_charge" to "1000",
                        "ev_max_charge" to "50000",
                        "ev_connector_types" to "ccs_combo_type1,ccs_combo_type2",
                        "energy_consumption_curve" to "0,300;20,160;80,140;120,180",
                        "ev_charging_curve" to "0,100000;40000,70000;60000,30000;80000,10000",
                        "ev_min_charge_at_charging_station" to "1",
                        "waypoints.charging_station_power" to ";$chargingStationPower;",
                        "waypoints.charging_station_current_type" to ";$chargingStationCurrentType;",
                        "waypoints.charging_station_id" to ";$chargingStationId;"
                    )
                )
                .build()
            val origin = routeOptions.coordinatesList().first()
            withMapboxNavigationAndOfflineTilesForRegion(
                OfflineRegions.Berlin,
                historyRecorderRule = mapboxHistoryTestRule
            ) { navigation ->
                navigation.registerRouteAlternativesObserver(
                    AdvancedAlternativesObserverFromDocumentation(navigation)
                )
                navigation.startTripSession()
                stayOnPosition(
                    origin.latitude(),
                    origin.longitude(),
                    originBearing,
                ) {
                    withoutInternet {
                        val requestResult = navigation.requestRoutes(routeOptions)
                            .getSuccessfulResultOrThrowException()
                        assertEquals(RouterOrigin.Onboard, requestResult.routerOrigin)
                        navigation.setNavigationRoutesAsync(requestResult.routes)
                        val offlinePrimaryRoute = requestResult.routes.first()
                        assertEquals(
                            listOf(null, "user-provided-charging-station", null),
                            offlinePrimaryRoute.getChargingStationsType()
                        )
                        assertEquals(
                            listOf(null, "$chargingStationPowerKw", null),
                            offlinePrimaryRoute.getChargingStationsPowerKw()
                        )
                        assertEquals(
                            listOf(null, chargingStationId, null),
                            offlinePrimaryRoute.getChargingStationsId()
                        )
                        assertEquals(
                            listOf(null, chargingStationCurrentType, null),
                            offlinePrimaryRoute.getChargingStationsCurrentType()
                        )
                    }
                    val onlineRoutes = navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                            it.navigationRoutes.first().origin == RouterOrigin.Offboard
                    }
                    val onlinePrimaryRoute = onlineRoutes.navigationRoutes.first()

                    assertEquals(
                        listOf(null, "user-provided-charging-station", null),
                        onlinePrimaryRoute.getChargingStationsType()
                    )
                    assertEquals(
                        listOf(null, "$chargingStationPowerKw", null),
                        onlinePrimaryRoute.getChargingStationsPowerKw()
                    )
                    assertEquals(
                        listOf(null, chargingStationId, null),
                        onlinePrimaryRoute.getChargingStationsId()
                    )
                    assertEquals(
                        listOf(null, chargingStationCurrentType, null),
                        onlinePrimaryRoute.getChargingStationsCurrentType()
                    )
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

private fun NavigationRoute.getChargingStationsType() = getWaypointMetadata("type")
private fun NavigationRoute.getChargingStationsPowerKw() = getWaypointMetadata("power_kw")
private fun NavigationRoute.getChargingStationsCurrentType() = getWaypointMetadata("current_type")
private fun NavigationRoute.getChargingStationsId() = getWaypointMetadata("station_id")

private fun NavigationRoute.getWaypointMetadata(name: String): List<String?> {
    return waypoints?.map {
        it.unrecognizedJsonProperties?.get("metadata")?.asJsonObject?.get(name)?.asString
    } ?: emptyList()
}
