package com.mapbox.navigation.utils.internal

import com.mapbox.common.Logger
import com.mapbox.navigation.testing.MockLoggerRule
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class LoggerProviderTest {

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    @Test
    fun `alias log V (called lvl D, Logger doesn't provide V)`() {
        logV("any_tag", "any_message")

        verify(exactly = 1) {
            Logger.d(any(), any())
        }
    }

    @Test
    fun `alias log D`() {
        logD("any_tag", "any_message")

        verify(exactly = 1) {
            Logger.d(any(), any())
        }
    }

    @Test
    fun `alias log I`() {
        logI("any_tag", "any_message")

        verify(exactly = 1) {
            Logger.i(any(), any())
        }
    }

    @Test
    fun `alias log W`() {
        logW("any_tag", "any_message")

        verify(exactly = 1) {
            Logger.w(any(), any())
        }
    }

    @Test
    fun `alias log E`() {
        logE("any_tag", "any_message")

        verify(exactly = 1) {
            Logger.e(any(), any())
        }
    }
}
