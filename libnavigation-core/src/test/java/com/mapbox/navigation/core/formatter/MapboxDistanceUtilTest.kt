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
            ctx,
        )

        assertEquals("12", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(12.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance large at lower bound value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            4828.03,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("3", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(3.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance large value UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            19312.1,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("12", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(12.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance large value UK at lower bound with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            4828.03,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("3", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(3.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance large value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            19312.1,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("19", result.distanceAsString)
        assertEquals("km", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(19.3121, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance large value metric at lower bound with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            3000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("3", result.distanceAsString)
        assertEquals("km", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(3.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance large value metric that is medium for imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            11000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("11", result.distanceAsString)
        assertEquals("km", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(11.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            55.3,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value metric at lower bound with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            0.0,
            Rounding.INCREMENT_FIVE,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("5", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(5.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value metric at upper bound with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            999.999,
            Rounding.INCREMENT_FIVE,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("1000", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(1000.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value metric that is medium for imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            390.0,
            Rounding.INCREMENT_FIVE,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("390", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(390.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            10.0,
            Rounding.INCREMENT_FIVE,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("30", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(30.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value imperial at lower bound with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            0.0,
            Rounding.INCREMENT_FIVE,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("5", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(5.0, result.distance, 0.0)
    }

    @Test
    fun `formatDistance small value imperial at upper bound with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            160.5,
            Rounding.INCREMENT_FIVE,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("525", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(525.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance small value UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            101.0,
            Rounding.INCREMENT_FIVE,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("110", result.distanceAsString)
        assertEquals("yd", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(110.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance small value at upper bound UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            160.5,
            Rounding.INCREMENT_FIVE,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("175", result.distanceAsString)
        assertEquals("yd", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(175.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance small value at lower bound UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            0.0,
            Rounding.INCREMENT_FIVE,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("5", result.distanceAsString)
        assertEquals("yd", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(5.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value imperial with specified locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            55.3,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
            Locale.JAPANESE,
        )

        assertEquals("150", result.distanceAsString)
        assertEquals("フィート", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(150.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium value at lower bound metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            1000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
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
            1100.5,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("1.1", result.distanceAsString)
        assertEquals("km", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(1.1005, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium value at upper bound metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            2904.89,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("2.9", result.distanceAsString)
        assertEquals("km", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(2.90489, result.distance, 0.0000001)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            1200.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("0.7", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(0.7456454, result.distance, 0.00001)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium value at upper bound imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            4741.9,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("2.9", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(2.94648, result.distance, 0.00001)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium value at lower bound imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            161.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("0.1", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(0.100041, result.distance, 0.00001)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance medium value UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            15000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("9", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(9.3205679, result.distance, 0.00001)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance medium value at lower bound UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            161.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("0.1", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(0.100041, result.distance, 0.00001)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance medium value at upper bound UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            4741.9,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("2.9", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(2.94648, result.distance, 0.00001)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance invalid large value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -19312.1,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance invalid medium value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -353.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance invalid small value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -54.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance invalid large value UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -19312.1,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("yd", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance invalid medium value UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -353.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("yd", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance invalid small value UK with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -54.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("yd", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance invalid large value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -19312.1,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance invalid medium value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -353.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance invalid small value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -54.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx,
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Test
    fun `formatDistance imperial return small distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            10.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL,
        )

        assertEquals(30.0, result, 0.0)
    }

    @Test
    fun `formatDistance imperial return small at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            0.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL,
        )

        assertEquals(10.0, result, 0.0)
    }

    @Test
    fun `formatDistance imperial return small at upper bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            4741.9,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL,
        )

        assertEquals(2.94648, result, 0.00001)
    }

    @Test
    fun `formatDistance imperial return small distance only with custom locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            10.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL,
            Locale.US,
        )

        assertEquals(30.0, result, 0.0)
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `formatDistance UK return small distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            101.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL,
        )

        assertEquals(110.0, result, 0.0)
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `formatDistance UK return small at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            0.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL,
        )

        assertEquals(10.0, result, 0.0)
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `formatDistance UK return small at upper bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            4741.9,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL,
        )

        assertEquals(2.94648, result, 0.00001)
    }

    @Test
    fun `formatDistance UK return small distance only with custom locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            101.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL,
            Locale("en", "GB"),
        )

        assertEquals(110.0, result, 0.0)
    }

    @Test
    fun `formatDistance imperial return small distance only when input negative`() {
        val result = MapboxDistanceUtil.formatDistance(
            -10.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `formatDistance UK return small distance only when input negative`() {
        val result = MapboxDistanceUtil.formatDistance(
            -10.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Test
    fun `formatDistance imperial return medium distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            13000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(8.0778255, result, 0.00001)
    }

    @Test
    fun `formatDistance imperial return medium at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            161.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(0.10004, result, 0.000001)
    }

    @Test
    fun `formatDistance imperial return medium at upper bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            4741.9,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(2.94648, result, 0.00001)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance UK return medium distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            10456.3,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(6.49724, result, 0.00001)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance UK return medium at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            161.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(0.10004, result, 0.000001)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance UK return medium at upper bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            4741.9,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(2.94648, result, 0.00001)
    }

    @Test
    fun `formatDistance imperial return large at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            4828.032,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(3.0, result, 0.00001)
    }

    @Test
    fun `formatDistance imperial return large distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            10800.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(6.7108112, result, 0.000001)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance UK return large distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            10800.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(6.7108089, result, 0.00001)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance UK return large at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            4828.032,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(3.0, result, 0.00001)
    }

    @Test
    fun `formatDistance imperial invalid large metric returns fifty`() {
        val result = MapboxDistanceUtil.formatDistance(
            -15000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance UK invalid large returns fifty`() {
        val result = MapboxDistanceUtil.formatDistance(
            -15000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid medium metric returns fifty`() {
        val result = MapboxDistanceUtil.formatDistance(
            -1500.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid medium imperial returns fifty`() {
        val result = MapboxDistanceUtil.formatDistance(
            -1500.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance invalid medium UK returns fifty`() {
        val result = MapboxDistanceUtil.formatDistance(
            -1500.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid small metric returns fifty`() {
        val result = MapboxDistanceUtil.formatDistance(
            -150.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid small imperial returns fifty`() {
        val result = MapboxDistanceUtil.formatDistance(
            -150.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Config(qualifiers = "en-rGB")
    @Test
    fun `formatDistance invalid small UK returns fifty`() {
        val result = MapboxDistanceUtil.formatDistance(
            -150.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Test
    fun `formatDistance metric return small distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            123.456,
            Rounding.INCREMENT_TEN,
            UnitType.METRIC,
        )

        assertEquals(120.0, result, 0.0)
    }

    @Test
    fun `formatDistance metric return small at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            0.0,
            Rounding.INCREMENT_TEN,
            UnitType.METRIC,
        )

        assertEquals(10.0, result, 0.0)
    }

    @Test
    fun `formatDistance metric return small at upper bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            999.999,
            Rounding.INCREMENT_TEN,
            UnitType.METRIC,
        )

        assertEquals(1000.0, result, 0.0)
    }

    @Test
    fun `formatDistance metric return small distance only when input negative`() {
        val result = MapboxDistanceUtil.formatDistance(
            -10.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(50.0, result, 0.0)
    }

    @Test
    fun `formatDistance metric return small distance only with custom locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            123.456,
            Rounding.INCREMENT_TEN,
            UnitType.METRIC,
            Locale.ENGLISH,
        )

        assertEquals(120.0, result, 0.0)
    }

    @Test
    fun `formatDistance metric return medium distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            2367.354,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(2.367354, result, 0.00001)
    }

    @Test
    fun `formatDistance metric return medium at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            1000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(1.0, result, 0.000001)
    }

    @Test
    fun `formatDistance metric return medium at upper bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            2910.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(2.91, result, 0.00001)
    }

    @Test
    fun `formatDistance metric return large at lower bound distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            3000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(3.0, result, 0.0)
    }

    @Test
    fun `formatDistance metric return large distance only`() {
        val result = MapboxDistanceUtil.formatDistance(
            10800.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
        )

        assertEquals(10.8, result, 0.0)
    }
}
