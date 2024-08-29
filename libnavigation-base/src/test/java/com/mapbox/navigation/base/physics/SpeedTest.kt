package com.mapbox.navigation.base.physics

import com.mapbox.navigation.base.physics.Speed.Companion.kph
import com.mapbox.navigation.base.physics.Speed.Companion.m_s
import com.mapbox.navigation.base.physics.Speed.Companion.mph
import com.mapbox.navigation.base.physics.Speed.Companion.toSpeed
import com.mapbox.navigation.base.speed.model.SpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTest {

    @Test
    fun `Check speed unit conversion`() {
        listOf(
            ConversionTestCase(
                100.0,
                SpeedUnit.KILOMETERS_PER_HOUR,
                27.7778,
                SpeedUnit.METERS_PER_SECOND,
            ),
            ConversionTestCase(
                100.0,
                SpeedUnit.KILOMETERS_PER_HOUR,
                62.1371,
                SpeedUnit.MILES_PER_HOUR,
            ),
            ConversionTestCase(
                100.0,
                SpeedUnit.KILOMETERS_PER_HOUR,
                100.0,
                SpeedUnit.KILOMETERS_PER_HOUR,
            ),

            ConversionTestCase(
                100.0,
                SpeedUnit.METERS_PER_SECOND,
                360.0,
                SpeedUnit.KILOMETERS_PER_HOUR,
            ),
            ConversionTestCase(
                100.0,
                SpeedUnit.METERS_PER_SECOND,
                223.6936,
                SpeedUnit.MILES_PER_HOUR,
            ),
            ConversionTestCase(
                100.0,
                SpeedUnit.METERS_PER_SECOND,
                100.0,
                SpeedUnit.METERS_PER_SECOND,
            ),

            ConversionTestCase(
                100.0,
                SpeedUnit.MILES_PER_HOUR,
                160.9344,
                SpeedUnit.KILOMETERS_PER_HOUR,
            ),
            ConversionTestCase(
                100.0,
                SpeedUnit.MILES_PER_HOUR,
                100.0,
                SpeedUnit.MILES_PER_HOUR,
            ),
            ConversionTestCase(
                100.0,
                SpeedUnit.MILES_PER_HOUR,
                44.704,
                SpeedUnit.METERS_PER_SECOND,
            ),
        ).forEach { (fromValue, fromUnit, toValue, toUnit) ->
            val speed = fromValue.toSpeed(fromUnit)
            assertEquals(
                "$speed.toDouble($toUnit) should be $toValue",
                toValue,
                speed.toDouble(toUnit),
                0.001,
            )
        }
    }

    @Test
    fun `Check arithmetic operation PLUS`() {
        val speed1 = 100.kph
        listOf(
            Triple(100, SpeedUnit.KILOMETERS_PER_HOUR, 200.0.kph),
            Triple(100, SpeedUnit.MILES_PER_HOUR, 260.9344.kph),
            Triple(100, SpeedUnit.METERS_PER_SECOND, 460.0.kph),
        ).forEach { (speed, unit, expectedSpeed) ->
            assertEquals(
                "$speed1 + $speed.toSpeed($unit) should be $expectedSpeed",
                expectedSpeed,
                speed1 + speed.toSpeed(unit),
            )
        }
    }

    @Test
    fun `Check arithmetic operation SUB`() {
        val speed1 = 200.kph
        listOf(
            Triple(100, SpeedUnit.KILOMETERS_PER_HOUR, 100.0.kph),
            Triple(100, SpeedUnit.MILES_PER_HOUR, 39.0656.kph),
            Triple(100, SpeedUnit.METERS_PER_SECOND, -(160.0.kph)),
        ).forEach { (speed, unit, expectedSpeed) ->
            assertEquals(
                "$speed1 - $speed.toSpeed($unit) should be $expectedSpeed",
                expectedSpeed,
                speed1 - speed.toSpeed(unit),
            )
        }
    }

    @Test
    fun `Check arithmetic operation MUL`() {
        val speed1 = 10.kph
        listOf(
            2 to 20.kph,
            10.5 to 105.kph,
            0.25 to 2.5.kph,
        ).forEach { (multiplier, expectedSpeed) ->
            assertEquals(
                "$speed1 * $multiplier should be $expectedSpeed",
                expectedSpeed,
                speed1 * multiplier,
            )
        }
    }

    @Test
    fun `Check arithmetic operation DIV`() {
        val speed1 = 10.kph
        listOf(
            2 to 5.kph,
            3 to 3.33333.kph,
            10 to 1.kph,
        ).forEach { (divisor, expectedSpeed) ->
            assertEquals(
                "$speed1 / $divisor should be $expectedSpeed",
                expectedSpeed,
                speed1 / divisor,
            )
        }

        listOf(
            100.kph to 0.1,
            1000.kph to 0.01,
            200.kph to 0.05,
        ).forEach { (divisor, expectedRatio) ->
            assertEquals(
                "$speed1 / $divisor should be $expectedRatio",
                expectedRatio,
                speed1 / divisor,
                0.00001,
            )
        }
    }

    @Test
    fun `Check boolean operations`() {
        val speed1 = 88.mph
        listOf(
            87.6543.mph to 1,
            88.0001.mph to -1,
            88.mph to 0,
        ).forEach { (speed, expected) ->
            assertEquals(
                "$speed1.compareTo($speed) should be $expected",
                expected,
                speed1.compareTo(speed),
            )
        }
    }

    @Test
    fun `zero equality check`() {
        listOf(
            Triple(Speed.ZERO, Speed.ZERO, true),
            Triple(0.kph, Speed.ZERO, true),
            Triple(0.mph, Speed.ZERO, true),
            Triple(0.m_s, Speed.ZERO, true),
            Triple(1.m_s, Speed.ZERO, false),
        ).forEach { (speed1, speed2, isEqual) ->
            assertEquals("$speed1 == $speed2 should be $isEqual", isEqual, speed1 == speed2)
        }
    }

    @Test
    fun `round to Int and Long`() {
        assertEquals(45, 44.99.mph.roundToInt(SpeedUnit.MILES_PER_HOUR))
        assertEquals(50, 50.4.kph.roundToInt(SpeedUnit.KILOMETERS_PER_HOUR))
        assertEquals(4, 3.5.m_s.roundToLong(SpeedUnit.METERS_PER_SECOND))
    }

    private data class ConversionTestCase(
        val fromValue: Double,
        val fromUnit: SpeedUnit,
        val toValue: Double,
        val toUnit: SpeedUnit,
    )
}
