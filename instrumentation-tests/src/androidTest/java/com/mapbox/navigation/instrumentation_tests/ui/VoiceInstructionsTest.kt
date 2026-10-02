package com.mapbox.navigation.instrumentation_tests.ui

import android.location.Location
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.replay.route.ReplayRouteSession
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.assertions.waitUntilHasSize
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class VoiceInstructionsTest : BaseCoreNoCleanUpTest() {

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

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun voiceInstructionIsDuplicatedOnceWhenReplayIsStarted() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule,
        ) { mapboxNavigation ->
            val mockRoute = RoutesProvider.dc_very_short(context)
            mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
            val voiceInstructions = mutableListOf<VoiceInstructions>()
            val voiceInstructionsObserver = VoiceInstructionsObserver {
                voiceInstructions.add(it)
            }
            val routes = mapboxNavigation.requestMockRoutes(
                mockWebServerRule,
                mockRoute,
            )
            mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutesAsync(routes)
            mapboxNavigation.moveAlongTheRouteUntilTracking(routes[0], mockLocationReplayerRule)
            voiceInstructions.waitUntilHasSize(1)
            val relayRouteSession = ReplayRouteSession()
            relayRouteSession.onAttached(mapboxNavigation)
            voiceInstructions.waitUntilHasSize(3, timeoutMillis = 15000)

            // the first instruction is duplicated once as a result of starting replay session
            assertEquals(voiceInstructions[0], voiceInstructions[1])
            // the first instruction id not duplicated anymore
            assertNotEquals(voiceInstructions[1], voiceInstructions[2])
        }
    }
}
