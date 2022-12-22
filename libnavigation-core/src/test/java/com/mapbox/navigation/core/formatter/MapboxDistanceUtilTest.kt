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
    fun `formatDistance large value metric that is medium for imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            11000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx
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
            ctx
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value metric that is medium for imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            390.0,
            Rounding.INCREMENT_FIVE,
            UnitType.METRIC,
            ctx
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
            ctx
        )

        assertEquals("30", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(30.0, result.distance, 0.1)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance small value close to upper bound imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            150.0,
            Rounding.INCREMENT_FIVE,
            UnitType.IMPERIAL,
            ctx
        )

        assertEquals("490", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(490.0, result.distance, 0.1)
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
    fun `formatDistance medium value close to upper bound imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            15000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx
        )

        assertEquals("9.3", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(9.3205679, result.distance, 0.00001)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance medium value close to lower bound imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            350.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx
        )

        assertEquals("0.2", result.distanceAsString)
        assertEquals("mi", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(0.21748, result.distance, 0.00001)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance invalid large value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -19312.1,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx
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
            ctx
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
            ctx
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance zero value imperial with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            0.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL,
            ctx
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("ft", result.distanceSuffix)
        assertEquals(UnitType.IMPERIAL, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
    }

    @Config(qualifiers = "en")
    @Test
    fun `formatDistance zero value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            0.0,
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
    fun `formatDistance invalid large value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -19312.1,
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
    fun `formatDistance invalid medium value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -353.0,
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
    fun `formatDistance invalid small value metric with default locale`() {
        val result = MapboxDistanceUtil.formatDistance(
            -54.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC,
            ctx
        )

        assertEquals("50", result.distanceAsString)
        assertEquals("m", result.distanceSuffix)
        assertEquals(UnitType.METRIC, result.unitType)
        assertEquals(50.0, result.distance, 0.0)
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
    fun `formatDistance return small distance only close to upper bound`() {
        val result = MapboxDistanceUtil.formatDistance(
            150.0,
            Rounding.INCREMENT_TEN,
            UnitType.IMPERIAL
        )

        assertEquals(490.0, result, 0.0)
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
    fun `formatDistance imperial return medium distance only close to lower bound`() {
        val result = MapboxDistanceUtil.formatDistance(
            350.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL
        )

        assertEquals(0.21748, result, 0.000001)
    }

    @Test
    fun `formatDistance imperial return medium distance only close to upper bound`() {
        val result = MapboxDistanceUtil.formatDistance(
            15000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL
        )

        assertEquals(9.3205679, result, 0.00001)
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

    @Test
    fun `formatDistance invalid large metric returns zero`() {
        val result = MapboxDistanceUtil.formatDistance(
            -15000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC
        )

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid large imperial returns zero`() {
        val result = MapboxDistanceUtil.formatDistance(
            -15000.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL
        )

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid medium metric returns zero`() {
        val result = MapboxDistanceUtil.formatDistance(
            -1500.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC
        )

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid medium imperial returns zero`() {
        val result = MapboxDistanceUtil.formatDistance(
            -1500.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL
        )

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid small metric returns zero`() {
        val result = MapboxDistanceUtil.formatDistance(
            -150.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.METRIC
        )

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `formatDistance invalid small imperial returns zero`() {
        val result = MapboxDistanceUtil.formatDistance(
            -150.0,
            Rounding.INCREMENT_FIFTY,
            UnitType.IMPERIAL
        )

        assertEquals(0.0, result, 0.0)
    }
}
