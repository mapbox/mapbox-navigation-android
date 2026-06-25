package com.mapbox.navigation.base.options

import android.content.Context
import android.text.format.DateFormat
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat.TWELVE_HOURS
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import com.mapbox.navigation.base.formatter.MapboxTimeFormatter
import com.mapbox.navigation.base.internal.time.TimeFormatter
import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.testing.assertIs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NavigationOptionsTest : BuilderTest<NavigationOptions, NavigationOptions.Builder>() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // timeFormatType is deprecated and only used to configure timeFormatter during build();
    // it is not stored directly in equals/hashCode.
    // timeFormatter is compared by reference; there are separate tests covering it.
    override val fieldsToExcludeFromEqualsHashCodeTest: Set<String> = setOf(
        "timeFormatType",
        "timeFormatter",
    )

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
            .timeFormatter(mockk())
            .distanceFormatter(mockk())
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
    fun defaultTimeFormatterWithCustomTimeFormatTypeIsUsed() {
        val options = NavigationOptions.Builder(context)
            .timeFormatType(TWELVE_HOURS)
            .build()

        assertIs<MapboxTimeFormatter>(options.timeFormatter)
        mockkStatic(TimeFormatter::class) {
            mockkStatic(DateFormat::is24HourFormat) {
                every { DateFormat.is24HourFormat(any()) } returns true
                every { TimeFormatter.formatTime(any(), any(), any()) } returns ""
                (options.timeFormatter as MapboxTimeFormatter).formatTime(mockk(relaxed = true))
                verify { TimeFormatter.formatTime(any(), TWELVE_HOURS, any()) }
            }
        }
    }

    @Test
    fun whenBuilderBuildWithNoValuesCalledThenDefaultValuesUsed() {
        val options = NavigationOptions.Builder(context).build()

        assertIs<MapboxTimeFormatter>(options.timeFormatter)
        assertNull(options.distanceFormatter)
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
        val timeFormatter = mockk<com.mapbox.navigation.base.formatter.TimeFormatter>()
        val navigatorPredictionMillis = 1020L

        val options = NavigationOptions.Builder(context)
            .timeFormatter(timeFormatter)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .build()

        val otherOptions = NavigationOptions.Builder(context)
            .timeFormatter(timeFormatter)
            .navigatorPredictionMillis(navigatorPredictionMillis)
            .build()

        assertEquals(options, otherOptions)
    }

    @Test
    fun equalsWithTheSameTimeFormatter() {
        val timeFormatter = mockk<com.mapbox.navigation.base.formatter.TimeFormatter>()

        val options1 = NavigationOptions.Builder(context).timeFormatter(timeFormatter).build()
        val options2 = NavigationOptions.Builder(context).timeFormatter(timeFormatter).build()

        assertEquals(options2, options1)
    }

    @Test
    fun equalsWithDifferentTimeFormatter() {
        val options1 = NavigationOptions.Builder(context).timeFormatter(mockk()).build()
        val options2 = NavigationOptions.Builder(context).timeFormatter(mockk()).build()

        assertNotEquals(options2, options1)
    }

    @Test
    fun hashCodeWithTheSameTimeFormatter() {
        val timeFormatter = mockk<com.mapbox.navigation.base.formatter.TimeFormatter>()

        val options1 = NavigationOptions.Builder(context).timeFormatter(timeFormatter).build()
        val options2 = NavigationOptions.Builder(context).timeFormatter(timeFormatter).build()

        assertEquals(options2.hashCode(), options1.hashCode())
    }

    @Test
    fun hashCodeWithDifferentTimeFormatter() {
        val options1 = NavigationOptions.Builder(context).timeFormatter(mockk()).build()
        val options2 = NavigationOptions.Builder(context).timeFormatter(mockk()).build()

        assertNotEquals(options2.hashCode(), options1.hashCode())
    }
}
