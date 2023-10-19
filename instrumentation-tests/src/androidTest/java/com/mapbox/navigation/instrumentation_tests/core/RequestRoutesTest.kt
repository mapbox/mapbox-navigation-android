package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.routes.EvRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockedEvRoutes
import com.mapbox.navigation.instrumentation_tests.utils.tiles.OfflineRegions
import com.mapbox.navigation.instrumentation_tests.utils.tiles.withMapboxNavigationAndOfflineTilesForRegion
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.instrumentation_tests.utils.withoutInternet
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestRoutesTest : BaseCoreNoCleanUpTest() {

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.361378213031003
            latitude = 52.49813341962201
        }
    }

    @Test
    fun buildOfflineRouteWithUnknownParameters() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
    ) {
        val testRoute = setupBerlinEvRoute()
        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
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
                                    "eta_model" to "enhanced"
                                )
                            )
                            .build()
                    ).getSuccessfulResultOrThrowException()
                    assertEquals(RouterOrigin.Onboard, requestResult.routerOrigin)
                    navigation.setNavigationRoutesAndWaitForUpdate(requestResult.routes)
                }
            }
        }
    }

    private fun setupBerlinEvRoute(): MockedEvRoutes {
        val originalTestRoute = EvRoutesProvider.getBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl
        )
        mockWebServerRule.requestHandlers.add(originalTestRoute.mockWebServerHandler)
        return originalTestRoute
    }
}
