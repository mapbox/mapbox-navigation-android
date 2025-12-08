package com.mapbox.navigation.base.options

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NavigationOptionsTest : BuilderTest<NavigationOptions, NavigationOptions.Builder>() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    override fun getImplementationClass(): KClass<NavigationOptions> = NavigationOptions::class

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun getFilledUpBuilder(): NavigationOptions.Builder {
        val context = mockk<Context>(relaxed = true)
        val appContext = mockk<Context>(relaxed = true)
        every { appContext.applicationContext } returns appContext
        every { context.applicationContext } returns appContext
        return NavigationOptions.Builder(context)
            .deviceProfile(mockk())
            .distanceFormatterOptions(mockk())
            .isDebugLoggingEnabled(true)
            .locationOptions(mockk())
            .navigatorPredictionMillis(1)
            .routingTilesOptions(mockk())
            .timeFormatType(1)
            .eHorizonOptions(mockk())
            .routeRefreshOptions(mockk())
            .rerouteOptions(mockk())
            .routeAlternativesOptions(mockk())
            .incidentsOptions(mockk())
            .historyRecorderOptions(
                HistoryRecorderOptions.Builder()
                    .fileDirectory("history/path")
                    .build(),
            )
            .eventsAppMetadata(
                EventsAppMetadata.Builder("name", "version")
                    .build(),
            )
            .enableSensors(true)
            .copilotOptions(
                CopilotOptions.Builder().shouldSendHistoryOnlyWithFeedback(true).build(),
            )
            .trafficOverrideOptions(
                TrafficOverrideOptions.Builder().isEnabled(true).build(),
            )
            .nativeRouteObject(true)
            .roadObjectMatcherOptions(
                RoadObjectMatcherOptions.Builder()
                    .openLRMaxDistanceToNode(newMaxDistance = 10.0)
                    .matchingGraphType(newType = NavigationTileDataDomain.NAVIGATION_HD)
                    .build(),
            )
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Test
    fun whenBuilderBuildWithNoValuesCalledThenDefaultValuesUsed() {
        val options = NavigationOptions.Builder(context).build()

        assertEquals(options.timeFormatType, NONE_SPECIFIED)
        assertEquals(options.navigatorPredictionMillis, DEFAULT_NAVIGATOR_PREDICTION_MILLIS)
        assertEquals(LocationOptions.Builder().build(), options.locationOptions)
        assertNotNull(options.routingTilesOptions)
    }

    @Test
    fun whenBuilderBuildCalledThenProperNavigationOptionsCreated() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L

        val options = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .build()

        assertEquals(options.timeFormatType, timeFormat)
        assertEquals(options.navigatorPredictionMillis, navigatorPredictionMillis)
    }

    @Test
    fun whenOptionsValuesChangedThenAllOtherValuesSaved() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L

        var options = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .build()

        val builder = options.toBuilder()
        val newTimeFormat = TWENTY_FOUR_HOURS
        val newNavigatorPredictionMillis = 900L
        options = builder
            .timeFormatType(newTimeFormat)
            .navigatorPredictionMillis(newNavigatorPredictionMillis)
            .build()

        assertEquals(options.timeFormatType, newTimeFormat)
        assertEquals(options.navigatorPredictionMillis, newNavigatorPredictionMillis)
    }

    @Test
    fun whenSeparateBuildersBuildSameOptions() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L

        val options = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .build()

        val otherOptions = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .build()

        assertEquals(options, otherOptions)
    }
}
