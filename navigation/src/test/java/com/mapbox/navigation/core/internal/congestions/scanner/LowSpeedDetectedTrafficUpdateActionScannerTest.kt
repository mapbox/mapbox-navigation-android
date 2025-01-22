package com.mapbox.navigation.core.internal.congestions.scanner

import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction
import com.mapbox.navigation.utils.internal.Time
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LowSpeedDetectedTrafficUpdateActionScannerTest {

    private val scanner = LowSpeedDetectedTrafficUpdateActionScanner()

    @Test
    fun `scanner should return accumulate actions once low speed detected`() = mockkObject(
        Time.SystemClockImpl,
    ) {
        val analysisResult = mockk<SpeedAnalysisResult.LowSpeedDetected> {
            every { resultElapsedMilliseconds } returns 1_000_000
        }

        every { Time.SystemClockImpl.millis() } returns 1_000_000

        val actual = scanner.scan(TrafficUpdateAction.NoAction, analysisResult)

        assertTrue(actual is TrafficUpdateAction.AccumulatingLowSpeed)
        (actual as TrafficUpdateAction.AccumulatingLowSpeed).let {
            assertEquals(20.seconds, it.timeUntilUpdate)
        }
    }

    @Test
    fun `scanner should continue to accumulate actions if less than 20 seconds passed`() = mockkObject(
        Time.SystemClockImpl,
    ) {
        val accumulationStartTime = 1_000_000 - 5 * 1_000
        val analysisResult = mockk<SpeedAnalysisResult.LowSpeedDetected>()
        val acc = mockk<TrafficUpdateAction.AccumulatingLowSpeed> {
            every { accumulationStart } returns accumulationStartTime.milliseconds
        }
        every { Time.SystemClockImpl.millis() } returns 1_000_000

        val actual = scanner.scan(acc, analysisResult)

        assertTrue(actual is TrafficUpdateAction.AccumulatingLowSpeed)
        (actual as TrafficUpdateAction.AccumulatingLowSpeed).let {
            assertEquals(15.seconds, it.timeUntilUpdate)
            assertEquals(accumulationStartTime.milliseconds, it.accumulationStart)
        }
    }

    @Test
    fun `scanner should return IncreaseTraffic actions if 20 seconds passed`() = mockkObject(
        Time.SystemClockImpl,
    ) {
        val accumulationStartTime = 1_000_000 - 21 * 1_000
        val analysisResult = mockk<SpeedAnalysisResult.LowSpeedDetected>(relaxed = true) {
            every { expectedCongestion } returns 50
        }
        val acc = mockk<TrafficUpdateAction.AccumulatingLowSpeed> {
            every { accumulationStart } returns accumulationStartTime.milliseconds
        }
        every { Time.SystemClockImpl.millis() } returns 1_000_000

        val actual = scanner.scan(acc, analysisResult)

        assertTrue(actual is TrafficUpdateAction.IncreaseTraffic)
        (actual as TrafficUpdateAction.IncreaseTraffic).let {
            assertEquals(50, it.expectedCongestion)
        }
    }
}
