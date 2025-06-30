@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.utils

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class NavigationPerformanceTest {

    private val mockLogger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(mockLogger)

    @Test
    fun `tracking performance in different logging states`() {
        PerformanceTracker.trackPerformanceSync("test-section1") { }
        NavigationPerformance.performanceInfoLoggingEnabled(true)
        PerformanceTracker.trackPerformanceSync("test-section2") { }
        NavigationPerformance.performanceInfoLoggingEnabled(false)
        PerformanceTracker.trackPerformanceSync("test-section3") { }

        verify(exactly = 0) { mockLogger.logI(match { it.contains("test-section1") }, any()) }
        verify(exactly = 2) { mockLogger.logI(match { it.contains("test-section2") }, any()) }
        verify(exactly = 0) { mockLogger.logI(match { it.contains("test-section3") }, any()) }
    }
}
