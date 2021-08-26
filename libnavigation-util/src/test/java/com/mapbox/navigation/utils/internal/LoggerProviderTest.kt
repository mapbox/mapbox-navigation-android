package com.mapbox.navigation.utils.internal

import com.mapbox.base.common.logger.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoggerProviderTest {

    private lateinit var mockkLogger: Logger

    @Before
    fun setup() {
        mockkLogger = mockk(relaxUnitFun = true)
        mockkObject(LoggerProvider)
        every { LoggerProvider.logger } returns mockkLogger
    }

    @After
    fun reset() {
        unmockkObject(LoggerProvider)
    }

    @Test
    fun `alias log V`() {
        logV(mockk(), mockk(), mockk())

        verify(exactly = 1) {
            mockkLogger.v(any(), any(), any())
        }
    }

    @Test
    fun `alias log D`() {
        logD(mockk(), mockk(), mockk())

        verify(exactly = 1) {
            mockkLogger.d(any(), any(), any())
        }
    }

    @Test
    fun `alias log I`() {
        logI(mockk(), mockk(), mockk())

        verify(exactly = 1) {
            mockkLogger.i(any(), any(), any())
        }
    }

    @Test
    fun `alias log W`() {
        logW(mockk(), mockk(), mockk())

        verify(exactly = 1) {
            mockkLogger.w(any(), any(), any())
        }
    }

    @Test
    fun `alias log E`() {
        logE(mockk(), mockk(), mockk())

        verify(exactly = 1) {
            mockkLogger.e(any(), any(), any())
        }
    }
}
