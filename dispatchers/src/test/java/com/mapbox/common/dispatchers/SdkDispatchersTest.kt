@file:Suppress("ForbiddenImport")
@file:OptIn(MapboxExperimental::class)

package com.mapbox.common.dispatchers

import android.util.Log
import com.mapbox.annotation.MapboxExperimental
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * [SdkDispatchers] is a process-wide singleton, so every test here resets its resolved
 * [SdkDispatchersConfig] before and after running to avoid leaking state into other tests
 * sharing the same test JVM.
 */
class SdkDispatchersTest {

    @Before
    @After
    fun resetSingletonState() {
        SdkDispatchers.resetConfigForTest()
        SdkDispatchers.resetTestDispatcher()
    }

    @Test
    fun defaultAndIO_fallBackToBuiltInDispatchers_whenConfigureIsNeverCalled() {
        // GIVEN

        // WHEN
        val default = SdkDispatchers.Default
        val io = SdkDispatchers.IO

        // THEN
        assertSame(Dispatchers.Default, default)
        assertSame(Dispatchers.IO, io)
    }

    @Test
    fun configure_appliesCustomDispatchers() {
        // GIVEN
        val customDispatcher = newSingleThreadContext("test")
        try {
            // WHEN
            SdkDispatchers.configure {
                setDefault(customDispatcher)
                setIO(customDispatcher)
            }

            // THEN
            assertSame(customDispatcher, SdkDispatchers.Default)
            assertSame(customDispatcher, SdkDispatchers.IO)
        } finally {
            customDispatcher.close()
        }
    }

    @Test
    fun configure_canBeCalledMultipleTimes_lastCallWins() {
        // GIVEN
        val first = newSingleThreadContext("first")
        val second = newSingleThreadContext("second")
        try {
            // WHEN
            SdkDispatchers.configure { setDefault(first) }
            SdkDispatchers.configure { setDefault(second) }

            // THEN
            assertSame(second, SdkDispatchers.Default)
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun configure_calledMoreThanOnce_logsError() {
        mockkStatic(Log::class) {
            every { Log.e(any(), any()) } returns 0

            // WHEN
            SdkDispatchers.configure { }
            SdkDispatchers.configure { }

            // THEN
            verify(exactly = 1) { Log.e("SdkDispatchers", any()) }
        }
    }

    @Test
    fun configure_calledAfterDefaultsAlreadyResolved_logsWarning() {
        mockkStatic(Log::class) {
            every { Log.w(any<String>(), any<String>()) } returns 0

            // GIVEN: defaults are resolved by reading Default
            SdkDispatchers.Default

            // WHEN
            SdkDispatchers.configure { }

            // THEN
            verify(exactly = 1) { Log.w("SdkDispatchers", any<String>()) }
        }
    }

    @Test
    fun configure_worksAfterDefaultAlreadyRead_subsequentReadsUseNewConfig() {
        // GIVEN
        assertSame(Dispatchers.Default, SdkDispatchers.Default)

        val customDispatcher = newSingleThreadContext("custom")
        try {
            // WHEN
            SdkDispatchers.configure { setDefault(customDispatcher) }

            // THEN
            assertSame(customDispatcher, SdkDispatchers.Default)
        } finally {
            customDispatcher.close()
        }
    }

    @Test
    fun configure_worksAfterIOAlreadyRead_subsequentReadsUseNewConfig() {
        // GIVEN
        assertSame(Dispatchers.IO, SdkDispatchers.IO)

        val customDispatcher = newSingleThreadContext("custom")
        try {
            // WHEN
            SdkDispatchers.configure { setIO(customDispatcher) }

            // THEN
            assertSame(customDispatcher, SdkDispatchers.IO)
        } finally {
            customDispatcher.close()
        }
    }

    @Test
    fun setTestDispatcher_overridesDefaultAndIO_untilReset() {
        // GIVEN
        val customDispatcher = newSingleThreadContext("test")
        try {
            // WHEN
            SdkDispatchers.setTestDispatcher(customDispatcher)

            // THEN
            assertSame(customDispatcher, SdkDispatchers.Default)
            assertSame(customDispatcher, SdkDispatchers.IO)

            // WHEN
            SdkDispatchers.resetTestDispatcher()

            // THEN
            assertSame(Dispatchers.Default, SdkDispatchers.Default)
        } finally {
            customDispatcher.close()
        }
    }

    @Test
    fun setTestDispatcher_takesPriorityOver_configuredDispatcher() {
        // GIVEN
        val configuredDispatcher = newSingleThreadContext("configured")
        val testDispatcher = newSingleThreadContext("test")
        try {
            SdkDispatchers.configure {
                setDefault(configuredDispatcher)
            }

            // WHEN
            SdkDispatchers.setTestDispatcher(testDispatcher)

            // THEN
            assertSame(testDispatcher, SdkDispatchers.Default)
        } finally {
            configuredDispatcher.close()
            testDispatcher.close()
        }
    }
}
