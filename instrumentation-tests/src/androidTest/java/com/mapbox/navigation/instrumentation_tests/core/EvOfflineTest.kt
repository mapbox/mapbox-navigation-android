package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.instrumentation_tests.utils.createTileStore
import com.mapbox.navigation.instrumentation_tests.utils.loadRegion
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.instrumentation_tests.utils.withoutInternet
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.NavigationRouteAlternativesResult
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.alternativesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvOfflineTest : BaseCoreNoCleanUpTest() {

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.361378213031003
            latitude = 52.49813341962201
        }
    }

    @Test
    fun requestRouteWithoutInternetAndTiles() = sdkTest {
        withMapboxNavigation { navigation ->
            withoutInternet {
                val routes = navigation.requestRoutes(routeInBerlin())
                assertTrue(routes is RouteRequestResult.Failure)
            }
        }
    }

    @Test
    fun startTripWithoutInternetThenTurnItOn() = sdkTest {
        withMapboxNavigationAndOfflineTilesForRegion(
            BERLIN_OFFLINE_REGION
        ) { navigation ->
            downloadBerlinRoutingTiles(navigation)
            navigation.startTripSession()
            val firstOnlineAlternative = async {
                navigation.alternativesUpdates()
                    .filterIsInstance<NavigationRouteAlternativesResult.OnRouteAlternatives>()
                    .filter { it.routerOrigin == RouterOrigin.Offboard }
                    .first()
            }
            withoutInternet {
                val requestResult = navigation.requestRoutes(routeInBerlin())
                    .getSuccessfulResultOrThrowException()
                assertEquals(RouterOrigin.Onboard, requestResult.routerOrigin)
                navigation.setNavigationRoutesAsync(requestResult.routes)
            }
            stayOnPosition(
                longitude = 13.361378213031003,
                latitude = 52.49813341962201
            )
            val onlineAlternative = firstOnlineAlternative.await()
            assertNotEquals(0, onlineAlternative.alternatives.size)
        }
    }


    private fun CoroutineScope.stayOnPosition(
        latitude: Double,
        longitude: Double,
    ) {
        launch {
            mockLocationUpdatesRule.pushLocationUpdate {
                this.latitude = latitude
                this.longitude = longitude
            }
        }
    }

}

private suspend inline fun BaseCoreNoCleanUpTest.withMapboxNavigationAndOfflineTilesForRegion(
    region: OfflineRegion,
    block: (MapboxNavigation) -> Unit
) {
    withMapboxNavigation(
        useRealTiles = true, //TODO: replace by offline tiles
        tileStore = createTileStore()
    ) { navigation ->
        downloadBerlinRoutingTiles(navigation)
        block(navigation)
    }
}


private suspend fun downloadBerlinRoutingTiles(navigation: MapboxNavigation) {
    loadRegion(navigation, BERLIN_GEOMETRY)
}

private data class OfflineRegion(
    val id: String,
    val geometry: Geometry
)



private fun routeInBerlin() = RouteOptions.builder()
    .applyDefaultNavigationOptions()
    .coordinates("13.361378213031003,52.49813341962201;13.393450988895268,52.50913924804004")
    .alternatives(true)
    .enableRefresh(true)
    .build()

private val BERLIN_GEOMETRY = FeatureCollection.fromJson(
    "{\n" +
        "  \"type\": \"FeatureCollection\",\n" +
        "  \"features\": [\n" +
        "    {\n" +
        "      \"type\": \"Feature\",\n" +
        "      \"properties\": {},\n" +
        "      \"geometry\": {\n" +
        "        \"coordinates\": [\n" +
        "          [\n" +
        "            [\n" +
        "              13.03807042990934,\n" +
        "              52.70072965030741\n" +
        "            ],\n" +
        "            [\n" +
        "              13.03807042990934,\n" +
        "              52.32726294794662\n" +
        "            ],\n" +
        "            [\n" +
        "              13.818542568562549,\n" +
        "              52.32726294794662\n" +
        "            ],\n" +
        "            [\n" +
        "              13.818542568562549,\n" +
        "              52.70072965030741\n" +
        "            ],\n" +
        "            [\n" +
        "              13.03807042990934,\n" +
        "              52.70072965030741\n" +
        "            ]\n" +
        "          ]\n" +
        "        ],\n" +
        "        \"type\": \"Polygon\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}"
).features()!!.first().geometry()!!

private val BERLIN_OFFLINE_REGION = OfflineRegion(
    id = "berlin-test-tiles",
    geometry = BERLIN_GEOMETRY
)