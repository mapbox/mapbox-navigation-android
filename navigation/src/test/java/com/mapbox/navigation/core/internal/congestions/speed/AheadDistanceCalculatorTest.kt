package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.navigation.core.internal.congestions.model.toMetersPerSecond
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class AheadDistanceCalculatorTest(
    private val speed: Double,
    private val minExpectedLengthInMeters: Int,
) {
    @Test
    fun `check ahead distance calculation`() {
        val calculator: AheadDistanceCalculator = PredictedTimeAheadDistanceCalculator()
        val actualValue = calculator(speed.toMetersPerSecond())
        assertTrue(
            "Incorrect calculation result. " +
                "Actual value: $actualValue Expected: $minExpectedLengthInMeters",
            actualValue >= minExpectedLengthInMeters,
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(25.0, 3000),
            arrayOf(27.7, 3320),
            arrayOf(33.3, 3990),
            arrayOf(36.1, 4330),
            arrayOf(41.6, 4992),
            arrayOf(45.0, 5400),
            arrayOf(51.0, 6000),
        )
    }
}
