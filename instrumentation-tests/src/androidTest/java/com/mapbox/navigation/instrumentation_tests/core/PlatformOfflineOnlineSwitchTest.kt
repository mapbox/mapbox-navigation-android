package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.routealternatives.OnlineRouteAlternativesSwitch
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.routes.EvRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockedEvRoutes
import com.mapbox.navigation.instrumentation_tests.utils.tiles.OfflineRegions
import com.mapbox.navigation.instrumentation_tests.utils.tiles.withMapboxNavigationAndOfflineTilesForRegion
import com.mapbox.navigation.instrumentation_tests.utils.withoutInternet
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

private const val EXPECTED_RETRY_TIME_AFTER_SERVER_ERROR = 60_000L
private const val ACCURACY_TIMEOUT = 5_000L
private const val TIME_FOR_ONE_ROUTE_REQUEST_TRY = 3_000L

@OptIn(ExperimentalMapboxNavigationAPI::class)
class PlatformOfflineOnlineSwitchTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.361378213031003
            latitude = 52.49813341962201
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
            OnlineRouteAlternativesSwitch().onAttached(navigation)
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
                }
                val onlineRoutes = navigation.routesUpdates().first {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                        it.navigationRoutes.first().origin == RouterOrigin.Offboard
                }
                assertEquals(
                    listOf(
                        "-aG_uwfS5gl5iXicU7UqdPUTpXY6MFXiiBOXy3_MZkpa4ySvR2WMUw==#0",
                        "-aG_uwfS5gl5iXicU7UqdPUTpXY6MFXiiBOXy3_MZkpa4ySvR2WMUw==#1",
                    ),
                    onlineRoutes.navigationRoutes.map { it.id }
                )
            }
        }
    }

    @Test
    fun startNavigationOfflineStayOfflineForAWhileThenTurnOnInternet() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
    ) {
        val originalTestRoute = setupBerlinEvRoute()
        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            OnlineRouteAlternativesSwitch(
                connectTimeoutMilliseconds = 400,
                readTimeoutMilliseconds = 1000,
                minimumRetryInterval = 500
            ).onAttached(navigation)
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
                    delay(4_000) // cause a few retry intervals to happen
                }
                val onlineRoutes = withTimeoutOrNull(2_000) {
                    navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                            it.navigationRoutes.first().origin == RouterOrigin.Offboard
                    }
                }
                assertNotNull(
                    "online routes weren't calculated in a reasonable time",
                    onlineRoutes
                )
                assertEquals(
                    listOf(
                        "-aG_uwfS5gl5iXicU7UqdPUTpXY6MFXiiBOXy3_MZkpa4ySvR2WMUw==#0",
                        "-aG_uwfS5gl5iXicU7UqdPUTpXY6MFXiiBOXy3_MZkpa4ySvR2WMUw==#1",
                    ),
                    onlineRoutes?.navigationRoutes?.map { it.id }
                )
            }
        }
    }

    @Test
    @Ignore("tests execution takes too long, run them locally")
    fun requestingOnlineRoutesAfterServerError() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE +
            EXPECTED_RETRY_TIME_AFTER_SERVER_ERROR +
            ACCURACY_TIMEOUT
    ) {
        var failRequestHandler: FailByRequestMockRequestHandler? = null
        val originalTestRoute = setupBerlinEvRoute {
            failRequestHandler = FailByRequestMockRequestHandler(it)
            failRequestHandler!!
        }

        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            OnlineRouteAlternativesSwitch(
                connectTimeoutMilliseconds = 400,
                readTimeoutMilliseconds = 1000,
                minimumRetryInterval = 500
            ).onAttached(navigation)
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
                    failRequestHandler!!.failResponse = true
                }
                delay(TIME_FOR_ONE_ROUTE_REQUEST_TRY)
                failRequestHandler!!.failResponse = false
                val onlineRoutesDuringDelay = withTimeoutOrNull(
                    EXPECTED_RETRY_TIME_AFTER_SERVER_ERROR / 2L
                ) {
                    navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                            it.navigationRoutes.first().origin == RouterOrigin.Offboard
                    }
                }
                assertNull(
                    "routes shouldn't be requested so fast after server error",
                    onlineRoutesDuringDelay
                )
                val onlineRoutesAfterDelay = withTimeoutOrNull(
                    EXPECTED_RETRY_TIME_AFTER_SERVER_ERROR / 2L
                ) {
                    navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                            it.navigationRoutes.first().origin == RouterOrigin.Offboard
                    }
                }
                assertNotNull(
                    "online routes weren't calculated after delay",
                    onlineRoutesAfterDelay
                )
                assertEquals(
                    listOf(
                        "-aG_uwfS5gl5iXicU7UqdPUTpXY6MFXiiBOXy3_MZkpa4ySvR2WMUw==#0",
                        "-aG_uwfS5gl5iXicU7UqdPUTpXY6MFXiiBOXy3_MZkpa4ySvR2WMUw==#1",
                    ),
                    onlineRoutesAfterDelay?.navigationRoutes?.map { it.id }
                )
            }
        }
    }

    @Test
    @Ignore("tests execution takes too long, run them locally")
    fun requestingOnlineRoutesForInvalidRouteRequest() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE +
            EXPECTED_RETRY_TIME_AFTER_SERVER_ERROR +
            ACCURACY_TIMEOUT
    ) {
        var failRequestHandler: FailByRequestMockRequestHandler? = null
        val originalTestRoute = setupBerlinEvRoute {
            failRequestHandler = FailByRequestMockRequestHandler(it)
            failRequestHandler!!
        }

        withMapboxNavigationAndOfflineTilesForRegion(
            OfflineRegions.Berlin,
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            OnlineRouteAlternativesSwitch(
                connectTimeoutMilliseconds = 400,
                readTimeoutMilliseconds = 1000,
                minimumRetryInterval = 500
            ).onAttached(navigation)
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
                    failRequestHandler!!.apply {
                        failResponse = true
                        failResponseCode = 400
                    }
                }
                delay(TIME_FOR_ONE_ROUTE_REQUEST_TRY)
                failRequestHandler!!.failResponse = false
                val onlineRoutesDuringDelay = withTimeoutOrNull(
                    EXPECTED_RETRY_TIME_AFTER_SERVER_ERROR +
                        ACCURACY_TIMEOUT
                ) {
                    navigation.routesUpdates().first {
                        it.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW &&
                            it.navigationRoutes.first().origin == RouterOrigin.Offboard
                    }
                }
                assertNull(
                    "routes shouldn't be requested after error 4xx",
                    onlineRoutesDuringDelay
                )
            }
        }
    }

    private fun setupBerlinEvRoute(
        requestHandlerInterceptor: (MockRequestHandler) -> MockRequestHandler = { it }
    ): MockedEvRoutes {
        val originalTestRoute = EvRoutesProvider.getBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl
        )
        mockWebServerRule.requestHandlers.add(
            requestHandlerInterceptor(originalTestRoute.mockWebServerHandler)
        )
        return originalTestRoute
    }
}
