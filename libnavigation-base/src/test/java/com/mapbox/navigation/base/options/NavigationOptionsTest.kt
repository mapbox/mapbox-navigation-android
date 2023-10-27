package com.mapbox.navigation.base.options

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NavigationOptionsTest : BuilderTest<NavigationOptions, NavigationOptions.Builder>() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(any()) } returns mockk()
    }

    @After
    fun teardown() {
        unmockkStatic(LocationEngineProvider::class)
    }

    override fun getImplementationClass(): KClass<NavigationOptions> = NavigationOptions::class

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun getFilledUpBuilder(): NavigationOptions.Builder {
        val context = mockk<Context>(relaxed = true)
        val appContext = mockk<Context>(relaxed = true)
        every { appContext.applicationContext } returns appContext
        every { context.applicationContext } returns appContext
        return NavigationOptions.Builder(context)
            .accessToken("pk.123")
            .deviceProfile(mockk())
            .distanceFormatterOptions(mockk())
            .isDebugLoggingEnabled(true)
            .locationEngine(mockk())
            .locationEngineRequest(
                LocationEngineRequest.Builder(1234L)
                    .setMaxWaitTime(2345L)
                    .setPriority(LocationEngineRequest.PRIORITY_LOW_POWER)
                    .setFastestInterval(3456L)
                    .setDisplacement(150.0f)
                    .build()
            )
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
                    .build()
            )
            .eventsAppMetadata(
                EventsAppMetadata.Builder("name", "version")
                    .build()
            )
            .enableSensors(true)
            .copilotOptions(
                CopilotOptions.Builder().shouldSendHistoryOnlyWithFeedback(true).build()
            )
            .longRoutesOptimisationOptions(
                LongRoutesOptimisationOptions.OptimiseNavigationForLongRoutes(
                    20 * 1024
                )
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

    @Test
    fun reuseChangedBuilder() {
        val builder = NavigationOptions.Builder(context)
        val options = builder.build()
        builder.accessToken("pk.123")

        assertNotEquals(options.toBuilder().build(), builder.build())
        assertEquals(options.toBuilder().build(), options)
    }
}
