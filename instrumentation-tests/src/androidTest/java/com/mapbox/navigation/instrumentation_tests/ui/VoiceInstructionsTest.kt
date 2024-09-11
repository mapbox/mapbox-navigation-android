package com.mapbox.navigation.instrumentation_tests.ui

import android.location.Location
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.replay.route.ReplayRouteSession
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.assertions.waitUntilHasSize
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI

class VoiceInstructionsTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @Before
    fun setup() {
        Espresso.onIdle()
    }

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
        val mockRoute = RoutesProvider.dc_very_short(context)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        val voiceInstructions = mutableListOf<VoiceInstructions>()
        val voiceInstructionsObserver = VoiceInstructionsObserver {
            voiceInstructions.add(it)
        }
        val mapboxNavigation = createMapboxNavigation()
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            mockRoute,
        )
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        stayOnPosition(
            mockRoute.routeWaypoints.first(),
            0f,
        ) {
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutesAsync(routes)
            voiceInstructions.waitUntilHasSize(1)
        }
        val relayRouteSession = ReplayRouteSession()
        relayRouteSession.onAttached(mapboxNavigation)
        voiceInstructions.waitUntilHasSize(3, timeoutMillis = 15000)

        // the first instruction is duplicated once as a result of starting replay session
        assertEquals(voiceInstructions[0], voiceInstructions[1])
        // the first instruction id not duplicated anymore
        assertNotEquals(voiceInstructions[1], voiceInstructions[2])
    }

    private fun createMapboxNavigation(): MapboxNavigation {
        val navigationOptions = NavigationOptions.Builder(context)
            .routingTilesOptions(
                RoutingTilesOptions.Builder()
                    .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                    .build(),
            )
            .build()
        return MapboxNavigationProvider.create(navigationOptions).also {
            mapboxHistoryTestRule.historyRecorder = it.historyRecorder
        }
    }
}
