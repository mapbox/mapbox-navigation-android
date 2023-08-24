@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.EvRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EvAlternativesTest : BaseCoreNoCleanUpTest() {

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
    fun passForkPointAndReceiveContinuousEvAlternative() = sdkTest {
        val originalTestRoute = EvRoutesProvider.getBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl
        )
        val continuousAlternative = EvRoutesProvider.getContinuousAlternativeBerlinEvRoute(
            context,
            mockWebServerRule.baseUrl
        )
        mockWebServerRule.requestHandlers.add(originalTestRoute.mockWebServerHandler)
        mockWebServerRule.requestHandlers.add(continuousAlternative.mockWebServerHandler)

        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(originalTestRoute.routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            navigation.startTripSession()
            mockLocationReplayerRule.playRoute(routes.first().directionsRoute)
            navigation.registerRouteAlternativesObserver(
                AdvancedAlternativesObserverFromDocumentation(navigation)
            )
            navigation.setNavigationRoutesAsync(routes)

            val newChargeLevel = "957"
            navigation.onEVDataUpdated(
                mapOf(
                    "ev_initial_charge" to newChargeLevel
                )
            )
            val newAlternative = navigation.routesUpdates()
                .first {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE &&
                        it.navigationRoutes.size == 2
                }
                .navigationRoutes[1]

            val alternativeRouteInitialChargeLevel = newAlternative.routeOptions
                .getUnrecognizedProperty("ev_initial_charge")
                ?.asString
            assertEquals(newChargeLevel, alternativeRouteInitialChargeLevel)
        }
    }
}
