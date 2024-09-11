package com.mapbox.navigation.core.formatter

import android.content.Context
import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.Rounding.INCREMENT_FIFTY
import com.mapbox.navigation.base.formatter.Rounding.INCREMENT_FIVE
import com.mapbox.navigation.base.formatter.UnitType
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxDistanceFormatterTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceImperialWithDefaultLocale() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(19312.1)

        assertEquals("12 mi", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceUnitTypeNull() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceUnitTypeEmptyString() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceMetric() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.METRIC)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceSmallDistanceMetric() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.METRIC)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(55.3)

        assertEquals("50 m", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceSmallDistanceImperial() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(INCREMENT_FIVE)
                .build(),
        ).formatDistance(10.0)

        assertEquals("30 ft", result.toString())
    }

    @Config(qualifiers = "ja")
    @Test
    fun formatDistanceJapaneseLocale() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(55.3)

        assertEquals("150 フィート", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumMetric() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.METRIC)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(1000.0)

        assertEquals("1 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumMetricFractionalValue() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.METRIC)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(1200.5)

        assertEquals("1.2 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumImperial() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(1000.0)

        assertEquals("0.6 mi", result.toString())
    }

    @Test
    fun getSpannableDistanceStringFormatsString() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .build(),
        ).getSpannableDistanceString(input)

        assertEquals("12 mi", result.toString())
    }

    @Test
    fun getSpannableDistanceStringHasCorrectNumberOfSpans() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .build(),
        ).getSpannableDistanceString(input)

        assertEquals(2, result.getSpans(0, result.count(), Object::class.java).size)
    }

    @Test
    fun getSpannableDistanceStringTypeFace() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .build(),
        ).getSpannableDistanceString(input)

        assertEquals(
            Typeface.BOLD,
            (result.getSpans(0, result.count(), Object::class.java)[0] as StyleSpan).style,
        )
    }

    @Test
    fun getSpannableDistanceStringRelativeSizeSpan() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).getSpannableDistanceString(input)

        assertEquals(
            0.75f,
            (
                result.getSpans(
                    0,
                    result.count(),
                    Object::class.java,
                )[1] as RelativeSizeSpan
                ).sizeChange,
        )
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceImperialWithNonDefaultLocale() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(INCREMENT_FIFTY)
                .locale(Locale("hu"))
                .build(),
        ).formatDistance(19312.1)

        assertEquals("12 mérföld", result.toString())
    }

    @Test
    fun builderUsesApplicationContext() {
        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.applicationContext } returns ctx

        MapboxDistanceFormatter(DistanceFormatterOptions.Builder(mockContext).build())

        verify(exactly = 1) { mockContext.applicationContext }
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceBelowZeroDistance() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(-0.1)

        assertEquals("$INCREMENT_FIFTY ft", result.toString())
    }

    @Config(qualifiers = "en-rUS")
    @Test
    fun formatDistanceUnitTypeUndefinedImperial() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(19312.1)

        assertEquals("12 mi", result.toString())
    }

    @Config(qualifiers = "jp-rJP")
    @Test
    fun formatDistanceUnitTypeUndefinedMetric() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .roundingIncrement(INCREMENT_FIFTY)
                .build(),
        ).formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Test
    fun formatDistanceDefaultBuilder() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx).build(),
        ).formatDistance(25.1)

        assertEquals("50 ft", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceZeroRounding() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(0)
                .build(),
        ).formatDistance(55.3)

        assertEquals("181 ft", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceZero() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .roundingIncrement(5)
                .build(),
        ).formatDistance(0.0)

        assertEquals("5 ft", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceZeroMetric() {
        val result = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.METRIC)
                .roundingIncrement(2)
                .build(),
        ).formatDistance(0.0)

        assertEquals("2 m", result.toString())
    }
}
