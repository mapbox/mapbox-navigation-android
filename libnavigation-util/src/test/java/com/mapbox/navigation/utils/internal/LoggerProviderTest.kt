package com.mapbox.navigation.utils.internal

import com.mapbox.common.Logger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoggerProviderTest {

    @Before
    fun setup() {
        mockkStatic(Logger::class)
        every { Logger.i(any(), any()) } just Runs
        every { Logger.w(any(), any()) } just Runs
        every { Logger.d(any(), any()) } just Runs
        every { Logger.e(any(), any()) } just Runs
        LoggerProvider.setLoggerFrontend(MapboxCommonLoggerFrontend())
    }

    @After
    fun tearDown() {
        unmockkStatic(Logger::class)
    }

    @Test
    fun `alias log V (called lvl D, Logger doesn't provide V)`() {
        logV("any_message", "any_clz_name")

        verify(exactly = 1) {
            Logger.d(any(), any())
        }
    }

    @Test
    fun `alias log D`() {
        logD("any_message", "any_clz_name")

        verify(exactly = 1) {
            Logger.d(any(), any())
        }
    }

    @Test
    fun `alias log I`() {
        logI("any_message", "any_clz_name")

        verify(exactly = 1) {
            Logger.i(any(), any())
        }
    }

    @Test
    fun `alias log W`() {
        logW("any_message", "any_clz_name")

        verify(exactly = 1) {
            Logger.w(any(), any())
        }
    }

    @Test
    fun `alias log E`() {
        logE("any_message", "any_clz_name")

        verify(exactly = 1) {
            Logger.e(any(), any())
        }
    }
}
