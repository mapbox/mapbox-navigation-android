package com.mapbox.navigation.core.formatter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.formatter.Rounding
import com.mapbox.navigation.base.formatter.UnitType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxDistanceUtilTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance large value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            19312.1,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx
        )

        assertEquals("12", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(12.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance large value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            19312.1,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx
        )

        assertEquals("19", result.distanceAsString)
        assertEquals("km", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(19.3121, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            55.3,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            10.0,
            Rounding.INCREMENT_FIVE,
            UnitType.IMPERIAL,
            ctx
        )

        assertEquals("30", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(30.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value imperial with specified locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            55.3,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
            Locale.JAPANESE
        )

        assertEquals("150", result.distanceAsString)
        assertEquals("フィート", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(150.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            1000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx
        )

        assertEquals("1", result.distanceAsString)
        assertEquals("km", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(1.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium fractional value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            400.5,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx
        )

        assertEquals("0.4", result.distanceAsString)
        assertEquals("km", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(0.4005, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            1000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx
        )

        assertEquals("0.6", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(0.6213714106386318, result.distance, 0.0)
    }

    @Test
    fun `formatDistance return small distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            10.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL
        )

        assertEquals(30.0, result, 0.0)
    }

    @Test
    fun `formatDistance return small distance only when input negative`() {
        val result = MapboxDistanceUtil.formatDistance(
            -10.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL
        )

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `formatDistance return large distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            1000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL
        )

        assertEquals(0.6213714106386318, result, 0.0)
    }
}
