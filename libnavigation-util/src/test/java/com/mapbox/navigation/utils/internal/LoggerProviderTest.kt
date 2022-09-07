package com.mapbox.navigation.utils.internal

import com.mapbox.common.NativeLoggerWrapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoggerProviderTest {

    @Before
    fun setup() {
        mockkObject(NativeLoggerWrapper)
        every { NativeLoggerWrapper.info(any(), any()) } just Runs
        every { NativeLoggerWrapper.warning(any(), any()) } just Runs
        every { NativeLoggerWrapper.debug(any(), any()) } just Runs
        every { NativeLoggerWrapper.error(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(NativeLoggerWrapper)
    }

    @Test
    fun `alias log V (called lvl D, Logger doesn't provide V)`() {
        logV("any_message", "any_clz_name")

        verify(exactly = 1) {
            NativeLoggerWrapper.debug(any(), any())
        }
    }

    @Test
    fun `alias log D`() {
        logD("any_message", "any_clz_name")

        verify(exactly = 1) {
            NativeLoggerWrapper.debug(any(), any())
        }
    }

    @Test
    fun `alias log I`() {
        logI("any_message", "any_clz_name")

        verify(exactly = 1) {
            NativeLoggerWrapper.info(any(), any())
        }
    }

    @Test
    fun `alias log W`() {
        logW("any_message", "any_clz_name")

        verify(exactly = 1) {
            NativeLoggerWrapper.warning(any(), any())
        }
    }

    @Test
    fun `alias log E`() {
        logE("any_message", "any_clz_name")

        verify(exactly = 1) {
            NativeLoggerWrapper.error(any(), any())
        }
    }
}
