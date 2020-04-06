package com.mapbox.navigation.core

import android.content.Context
import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.typedef.IMPERIAL
import com.mapbox.navigation.base.typedef.METRIC
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxDistanceFormatterTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceImperialWithDefaultLocale() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(IMPERIAL)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("12 mi", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceUnitTypeNull() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceUnitTypeEmptyString() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType("")
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceMetric() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(METRIC)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceSmallDistanceMetric() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(METRIC)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(10.0)

        assertEquals("50 m", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceSmallDistanceImperial() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(IMPERIAL)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(10.0)

        assertEquals("50 ft", result.toString())
    }

    @Config(qualifiers = "ja")
    @Test
    fun formatDistanceJapaneseLocale() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(IMPERIAL)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(10.0)

        assertEquals("50 フィート", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumMetric() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(METRIC)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(1000.0)

        assertEquals("1 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumMetricFractionalValue() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(METRIC)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .formatDistance(400.5)

        assertEquals("0.4 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceMediumImperial() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(IMPERIAL)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
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

        assertEquals(Typeface.BOLD, (result.getSpans(0, result.count(), Object::class.java)[0] as StyleSpan).style)
    }

    @Test
    fun getSpannableDistanceStringRelativeSizeSpan() {
        val input = Pair("12", "mi")

        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(IMPERIAL)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .build()
            .getSpannableDistanceString(input)

        assertEquals(0.65f, (result.getSpans(0, result.count(), Object::class.java)[1] as RelativeSizeSpan).sizeChange)
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceImperialWithNonDefaultLocale() {
        val result = MapboxDistanceFormatter.Builder(ctx)
            .withUnitType(IMPERIAL)
            .withRoundingIncrement(ROUNDING_INCREMENT_FIFTY)
            .withLocale(Locale("hu"))
            .build()
            .formatDistance(19312.1)

        assertEquals("12 mérföld", result.toString())
    }
}
