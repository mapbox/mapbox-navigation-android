package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.coroutines.bannerInstructions
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.voiceInstructions
import com.mapbox.navigation.testing.ui.utils.coroutines.withLogOnTimeout
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

class BannerAndVoiceInstructionsTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        val origin = testRoute().routeWaypoints.first()
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
    }

    @Test
    fun departure_banner_and_voice_instructions() = sdkTest {
        val testRoutes = testRoute().toNavigationRoutes()
        val mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .historyRecorderOptions(
                    HistoryRecorderOptions.Builder()
                        .build()
                )
                .build()
        )
        mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
        mapboxNavigation.historyRecorder.startRecording()

        val bannerInstructionDeferred = async { mapboxNavigation.bannerInstructions().first() }
        val voiceInstructionDeferred = async { mapboxNavigation.voiceInstructions().first() }
        mapboxNavigation.setNavigationRoutes(testRoutes)
        mapboxNavigation.startTripSession()
        val bannerInstruction = withLogOnTimeout("waiting for an initial banner instruction") {
            bannerInstructionDeferred.await()
        }
        val voiceInstruction = withLogOnTimeout("waiting for an initial voice instruction") {
            voiceInstructionDeferred.await()
        }

        assertEquals("Pennsylvania Avenue Northwest", bannerInstruction.primary().text())
        assertEquals(ManeuverModifier.RIGHT, bannerInstruction.primary().modifier())
        assertEquals(
            "Drive north on 14th Street Northwest. " +
                "Then Turn right onto Pennsylvania Avenue Northwest.",
            voiceInstruction.announcement()
        )
    }

    private fun testRoute() = RoutesProvider.dc_very_short(context)
}
