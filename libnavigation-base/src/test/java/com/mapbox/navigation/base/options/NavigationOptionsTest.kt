package com.mapbox.navigation.base.options

import android.content.Context
import android.text.SpannableString
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import com.mapbox.navigation.base.formatter.DistanceFormatter
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
import kotlin.reflect.KClass

class NavigationOptionsTest : BuilderTest<NavigationOptions, NavigationOptions.Builder>() {

    private val context: Context = mockk()

    @Before
    fun setup() {
        every { context.applicationContext } returns context

        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(any()) } returns mockk()
    }

    @After
    fun teardown() {
        unmockkStatic(LocationEngineProvider::class)
    }

    override fun getImplementationClass(): KClass<NavigationOptions> = NavigationOptions::class

    override fun getFilledUpBuilder(): NavigationOptions.Builder {
        val context = mockk<Context>()
        val appContext = mockk<Context>(relaxed = true)
        every { appContext.applicationContext } returns appContext
        every { context.applicationContext } returns appContext
        return NavigationOptions.Builder(context)
            .accessToken("pk.123")
            .deviceProfile(mockk())
            .distanceFormatter(mockk())
            .isDebugLoggingEnabled(true)
            .isFromNavigationUi(true)
            .locationEngine(mockk())
            .navigatorPredictionMillis(1)
            .onboardRouterOptions(mockk())
            .timeFormatType(1)
            .eHorizonOptions(mockk())
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
        assertEquals(options.distanceFormatter, null)
        assertNotNull(options.onboardRouterOptions)
    }

    @Test
    fun whenBuilderBuildCalledThenProperNavigationOptionsCreated() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L
        val distanceFormatter = object : DistanceFormatter {
            override fun formatDistance(distance: Double): SpannableString {
                throw NotImplementedError()
            }
        }

        val options = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .distanceFormatter(distanceFormatter)
            .build()

        assertEquals(options.timeFormatType, timeFormat)
        assertEquals(options.navigatorPredictionMillis, navigatorPredictionMillis)
        assertEquals(options.distanceFormatter, distanceFormatter)
    }

    @Test
    fun whenOptionsValuesChangedThenAllOtherValuesSaved() {
        val timeFormat = TWELVE_HOURS
        val navigatorPredictionMillis = 1020L
        val distanceFormatter = object : DistanceFormatter {
            override fun formatDistance(distance: Double): SpannableString {
                throw NotImplementedError()
            }
        }

        var options = NavigationOptions.Builder(context)
            .timeFormatType(timeFormat)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .distanceFormatter(distanceFormatter)
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
        assertEquals(options.distanceFormatter, distanceFormatter)
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
