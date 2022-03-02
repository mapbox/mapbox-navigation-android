package com.mapbox.navigation.testing

import com.mapbox.common.Logger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MockLoggerRule : TestWatcher() {

    override fun starting(description: Description?) {
        mockkStatic(Logger::class)
        every { Logger.i(any(), any()) } just Runs
        every { Logger.w(any(), any()) } just Runs
        every { Logger.d(any(), any()) } just Runs
        every { Logger.e(any(), any()) } just Runs
    }

    override fun finished(description: Description?) {
        unmockkStatic(Logger::class)
    }
}
