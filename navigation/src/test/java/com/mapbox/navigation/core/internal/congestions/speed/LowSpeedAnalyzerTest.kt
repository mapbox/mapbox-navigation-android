package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.internal.congestions.model.MetersPerSecond
import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class LowSpeedAnalyzerTest {

    @Test
    fun `expected congestion for current speed matches with real congestion`() {
        val analyzer = LowSpeedAnalyzer()
        val routeLegProgress = mockk<RouteLegProgress> {
            every { routeLeg } returns mockk {
                every { annotation() } returns mockk {
                    every { congestionNumeric() } returns listOf(90, 90, 90)
                }
            }
            every { geometryIndex } returns 0
        }
        val navigationRoute = mockk<NavigationRoute>()
        val currentSpeed = MetersPerSecond.fromKilometersPerHour(10f)
        val expectedSpeed = MetersPerSecond.fromKilometersPerHour(90f)

        val result = analyzer(routeLegProgress, navigationRoute, currentSpeed, expectedSpeed)

        assertEquals(
            SpeedAnalysisResult.SpeedMatchesCongestionLevel(currentSpeed, 88, 90),
            result,
        )
    }

    @Test
    fun `expected congestion for current speed doesn't match with real congestion`() {
        val analyzer = LowSpeedAnalyzer()
        val routeLegProgress = mockk<RouteLegProgress> {
            every { routeLeg } returns mockk {
                every { annotation() } returns mockk {
                    every { congestionNumeric() } returns listOf(0, 0, 0)
                }
            }
            every { geometryIndex } returns 0
        }
        val navigationRoute = mockk<NavigationRoute>()
        val currentSpeed = MetersPerSecond.fromKilometersPerHour(10f)
        val expectedSpeed = MetersPerSecond.fromKilometersPerHour(90f)

        val result = analyzer(routeLegProgress, navigationRoute, currentSpeed, expectedSpeed)

        assertTrue(result is SpeedAnalysisResult.LowSpeedDetected)
    }
}
