package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import com.mapbox.navigation.testing.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class SanityCoreRouteTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        val mockRoute = RoutesProvider.dc_very_short(context)
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
    }

    @Test
    fun route_completes() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val mockRoute = RoutesProvider.dc_very_short(context)
            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

            val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
                requiredState(RouteProgressState.TRACKING)
                requiredState(RouteProgressState.COMPLETE)
            }

            mapboxNavigation.startTripSession()
            val routeOptions = RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints).build()

            val routes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            mapboxNavigation.setNavigationRoutes(routes)
            mockLocationReplayerRule.playRoute(routes[0].directionsRoute)

            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentState == RouteProgressState.COMPLETE }
                .first()

            mapboxNavigation.historyRecorder.stopRecording()
            expectedStates.assert()
        }
    }

    @Test
    fun route_with_two_legs_completes() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val mockRoute = RoutesProvider.dc_very_short_two_legs(context)
            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

            val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
                requiredState(RouteProgressState.TRACKING)
                requiredState(RouteProgressState.COMPLETE)
                requiredState(RouteProgressState.TRACKING)
                requiredState(RouteProgressState.COMPLETE)
            }

            mapboxNavigation.startTripSession()

            val routeOptions = RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .baseUrl(mockWebServerRule.baseUrl)
                .coordinatesList(mockRoute.routeWaypoints)
                .build()

            val routes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            mapboxNavigation.setNavigationRoutes(routes)
            mockLocationReplayerRule.playRoute(routes.first().directionsRoute)

            mapboxNavigation.flowOnFinalDestinationArrival().first()
            mapboxNavigation.historyRecorder.stopRecording()

            expectedStates.assert()
        }
    }
}
