package com.mapbox.navigation.core.internal.formatter

import android.content.Context
import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.internal.VoiceUnit.IMPERIAL
import com.mapbox.navigation.base.internal.VoiceUnit.METRIC
import com.mapbox.navigation.base.internal.VoiceUnit.UNDEFINED
import com.mapbox.navigation.core.Rounding.INCREMENT_FIFTY
import com.mapbox.navigation.core.Rounding.INCREMENT_FIVE
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxDistanceFormatterTest :
    BuilderTest<MapboxDistanceFormatter, MapboxDistanceFormatter.Builder>() {

    private lateinit var ctx: Context

    override fun getImplementationClass(): KClass<MapboxDistanceFormatter> =
        MapboxDistanceFormatter::class

    override fun getFilledUpBuilder(): MapboxDistanceFormatter.Builder {
        return MapboxDistanceFormatter.Builder(ctx)
            .locale(Locale("hu"))
            .roundingIncrement(123)
            .unitType("unitType")
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceImperialWithDefaultLocale() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(IMPERIAL)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("12 mi", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceUnitTypeNull() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceUnitTypeEmptyString() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType("")
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceMetric() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(METRIC)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceSmallDistanceMetric() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(METRIC)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(10.0)

        assertEquals("50 m", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceSmallDistanceImperial() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(IMPERIAL)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(10.0)

        assertEquals("50 ft", result.toString())
    }

    @Config(qualifiers = "ja")
    @Test
    fun formatDistanceJapaneseLocale() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(IMPERIAL)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(10.0)

        assertEquals("50 フィート", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumMetric() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(METRIC)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(1000.0)

        assertEquals("1 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumMetricFractionalValue() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(METRIC)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(400.5)

        assertEquals("0.4 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumImperial() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(IMPERIAL)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(1000.0)

        assertEquals("0.6 mi", result.toString())
    }

    @Test
    fun getSpannableDistanceStringFormatsString() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter.Builder(ctx)
            .build()
            .getSpannableDistanceString(input)

        assertEquals("12 mi", result.toString())
    }

    @Test
    fun getSpannableDistanceStringHasCorrectNumberOfSpans() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter.Builder(ctx)
            .build()
            .getSpannableDistanceString(input)

        assertEquals(2, result.getSpans(0, result.count(), Object::class.java).size)
    }

    @Test
    fun getSpannableDistanceStringTypeFace() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter.Builder(ctx)
            .build()
            .getSpannableDistanceString(input)

        assertEquals(
            Typeface.BOLD,
            (result.getSpans(0, result.count(), Object::class.java)[0] as StyleSpan).style
        )
    }

    @Test
    fun getSpannableDistanceStringRelativeSizeSpan() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(IMPERIAL)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .getSpannableDistanceString(input)

        assertEquals(
            0.65f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java
                )[1] as RelativeSizeSpan
                ).sizeChange
        )
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceImperialWithNonDefaultLocale() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(IMPERIAL)
            .roundingIncrement(INCREMENT_FIFTY)
            .locale(Locale("hu"))
            .build()
            .formatDistance(19312.1)

        assertEquals("12 mérföld", result.toString())
    }

    @Test
    fun builderUsesApplicationContext() {
        val mockContext = mockk<Context>()
        every { mockContext.applicationContext } returns ctx

        MapboxDistanceFormatter.Builder(mockContext).build()

        verify(exactly = 1) { mockContext.applicationContext }
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceBelowZeroDistance() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(IMPERIAL)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(-0.1)

        assertEquals("50 ft", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceBelowZeroDistanceRoundingIncrementFive() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(IMPERIAL)
            .roundingIncrement(INCREMENT_FIVE)
            .build()
            .formatDistance(-0.1)

        assertEquals("5 ft", result.toString())
    }

    @Config(qualifiers = "en-rUS")
    @Test
    fun formatDistanceUnitTypeUndefinedImperial() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(UNDEFINED)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("12 mi", result.toString())
    }

    @Config(qualifiers = "jp-rJP")
    @Test
    fun formatDistanceUnitTypeUndefinedMetric() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .unitType(UNDEFINED)
            .roundingIncrement(INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Test
    fun formatDistanceDefaultBuilder() {
        val distanceFormatter = MapboxDistanceFormatter.Builder(ctx)
            .build()

        val result = distanceFormatter.formatDistance(25.1)

        assertEquals("50 ft", result.toString())
    }
}
