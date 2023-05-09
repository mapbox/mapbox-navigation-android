package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvOfflineTest : BaseCoreNoCleanUpTest() {

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.361378213031003
            latitude = 52.49813341962201
        }
    }

    @Before
    fun setupMockRoutes() {
        val evRouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = readRawFileText(context, R.raw.ev_routes_berlin),
            expectedCoordinates = evRouteInBerlin().coordinatesList(),
        )
        mockWebServerRule.requestHandlers.add(evRouteRequestHandler)
    }

    @Test
    fun requestRouteWithoutInternetAndTiles() = sdkTest {
        withMapboxNavigation { navigation ->
            withoutInternet {
                val routes = navigation.requestRoutes(evRouteInBerlin())
                assertTrue(routes is RouteRequestResult.Failure)
            }
        }
    }

    @Test
    fun startTripWithoutInternetThenTurnItOn() = sdkTest {
        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin
        ) { navigation ->
            navigation.startTripSession()
            withoutInternet {
                val requestResult = navigation.requestRoutes(evRouteInBerlin())
                    .getSuccessfulResultOrThrowException()
                assertEquals(RouterOrigin.Onboard, requestResult.routerOrigin)
                navigation.setNavigationRoutesAsync(requestResult.routes)
            }
            stayOnPosition(
                longitude = 13.361378213031003,
                latitude = 52.49813341962201
            ) {
                val onlineAlternative = navigation.alternativesUpdates()
                    .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
                    .filter { it.routerOrigin == RouterOrigin.Offboard }
                    .first()
                assertNotEquals(0, onlineAlternative.alternatives.size)
            }
        }
    }

    @Test
    fun deviateFromOnlinePrimaryRouteWithoutInternet() = sdkTest {
        val newRouteOnlineRouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = readRawFileText(context, R.raw.ev_routes_berlin_reroute),
            expectedCoordinates = listOf(
                Point.fromLngLat(13.36742058325467, 52.49745756017697),
                evRouteInBerlin().coordinatesList().last()
            )
        )
        mockWebServerRule.requestHandlers.add(newRouteOnlineRouteRequestHandler)

        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin
        ) { navigation ->
            navigation.startTripSession()
            val requestResult = navigation.requestRoutes(evRouteInBerlin())
                .getSuccessfulResultOrThrowException()
            assertEquals(RouterOrigin.Offboard, requestResult.routerOrigin)
            navigation.setNavigationRoutesAsync(requestResult.routes)
            withoutInternet {
                stayOnPosition( //off route position
                    longitude = 13.36742058325467,
                    latitude = 52.49745756017697
                ) {
                    val newRoutes = navigation.routesUpdates()
                        .first { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
                    assertEquals(RouterOrigin.Onboard, newRoutes.navigationRoutes.first().origin)
                }
            }
            stayOnPosition( // origin position after reroute
                longitude = 13.36742058325467,
                latitude = 52.49745756017697
            ) {
                // TODO: NN doesn't respect base url and always uses api.mapbox.com
                val firstOnlineAlternative = navigation.alternativesUpdates()
                    .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
                    .filter { it.routerOrigin == RouterOrigin.Offboard }
                    .first()
                assertNotEquals(0, firstOnlineAlternative.alternatives.size)
            }
        }
    }

    private fun evRouteInBerlin() = RouteOptions.builder()
        .applyDefaultNavigationOptions()
        .baseUrl(mockWebServerRule.baseUrl) // comment to use real server
        .coordinates("13.361378213031003,52.49813341962201;13.393450988895268,52.50913924804004")
        .annotations("state_of_charge")
        .alternatives(true)
        .waypointsPerRoute(true)
        .unrecognizedProperties(
            mapOf(
                "engine" to "electric",
                "ev_initial_charge" to "6000",
                "ev_max_charge" to "50000",
                "ev_connector_types" to "ccs_combo_type1,ccs_combo_type2",
                "energy_consumption_curve" to "0,300;20,160;80,140;120,180",
                "ev_charging_curve" to "0,100000;40000,70000;60000,30000;80000,10000",
                "ev_min_charge_at_charging_station" to "1"
            )
        )
        .build()
}
