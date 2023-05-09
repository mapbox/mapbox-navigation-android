package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Value
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.instrumentation_tests.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.instrumentation_tests.utils.loadRegion
import com.mapbox.navigation.instrumentation_tests.utils.withoutInternet
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvOfflineTest : BaseCoreNoCleanUpTest() {

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 4.898473756907066
            latitude = 52.37373595766587
        }
    }

    @Test
    fun requestRouteWithoutInternetAndTiles() = sdkTest {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinates(
                "4.898473756907066,52.37373595766587" +
                    ";5.359980783143584,43.280050656855906"
            )
            .alternatives(true)
            .enableRefresh(true)
            .build()

        withMapboxNavigation { navigation ->
            withoutInternet {
                val routes = navigation.requestRoutes(routeOptions)
                assertTrue(routes is RouteRequestResult.Failure)
            }
        }
    }

    @Test
    fun requestRouteWithoutInternetHavingTiles() = sdkTest {
        val tileStore = TileStore.create()
        tileStore.setOption(
            TileStoreOptions.MAPBOX_ACCESS_TOKEN,
            TileDataDomain.NAVIGATION,
            Value.valueOf(getMapboxAccessTokenFromResources(context))
        )
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinates("13.361378213031003,52.49813341962201;13.393450988895268,52.50913924804004")
            .alternatives(true)
            .enableRefresh(true)
            .build()

        withMapboxNavigation(
            useRealTiles = true,
            tileStore = tileStore
        ) { navigation ->
            loadRegion(navigation, BERLIN_GEOMETRY)
            withoutInternet {
                val routes = navigation.requestRoutes(routeOptions)
                    .getSuccessfulResultOrThrowException()
                assertEquals(RouterOrigin.Onboard, routes.routerOrigin)
            }
        }
    }
}

private val BERLIN_GEOMETRY = FeatureCollection.fromJson("{\n" +
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
    "}").features()!!.first().geometry()!!