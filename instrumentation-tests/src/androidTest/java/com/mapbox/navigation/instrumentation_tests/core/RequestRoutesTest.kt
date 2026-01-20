package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.common.TileDataDomain
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.utils.createTileStore
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.offline.Tileset
import com.mapbox.navigation.testing.utils.offline.unpackTiles
import com.mapbox.navigation.testing.utils.routes.EvRoutesProvider
import com.mapbox.navigation.testing.utils.routes.MockedEvRoutes
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import com.mapbox.navigation.testing.utils.withoutInternet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class RequestRoutesTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.361378213031003
            latitude = 52.49813341962201
        }
    }

    @Test
    fun buildOfflineRouteWithUnknownParameters() = sdkTest {
        val testRoute = setupBerlinEvRoute()
        val tilesVersion = context.unpackTiles(Tileset.Berlin)[TileDataDomain.NAVIGATION]!!
        withMapboxNavigation(
            tileStore = createTileStore(),
            tilesVersion = tilesVersion,
            deviceType = DeviceType.AUTOMOBILE,
            historyRecorderRule = mapboxHistoryTestRule,
        ) { navigation ->
            navigation.startTripSession()
            stayOnPosition(
                testRoute.origin.latitude(),
                testRoute.origin.longitude(),
                0.0f,
            ) {
                withoutInternet {
                    val requestResult = navigation.requestRoutes(
                        testRoute.routeOptions.toBuilder()
                            .unrecognizedProperties(
                                mapOf(
                                    "unknown_key1" to "unknown_value1",
                                    "unknown_key2" to "333",
                                    "eta_model" to "enhanced",
                                ),
                            )
                            .build(),
                    ).getSuccessfulResultOrThrowException()
                    assertEquals(RouterOrigin.OFFLINE, requestResult.routerOrigin)
                    navigation.setNavigationRoutesAndWaitForUpdate(requestResult.routes)
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
}
