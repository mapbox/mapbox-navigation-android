package com.mapbox.navigation.logger

import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class MapboxLoggerTest {

    private lateinit var logger: MapboxLogger

    @Before
    fun setUp() {
        mockkStatic(Timber::class)
        logger = MapboxLogger.instance
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(logger)
    }

    @Test
    fun verboseTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = VERBOSE

        logger.v("TAG", "some message", throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.v(throwable, "some message") }
        verify { loggerObserver.log(VERBOSE, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun verboseTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = DEBUG

        logger.v("TAG", "some message", throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.v(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun debugTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = DEBUG

        logger.d("TAG", "some message", throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.d(throwable, "some message") }
        verify { loggerObserver.log(DEBUG, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun debugTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = INFO

        logger.d("TAG", "some message", throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.d(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun infoTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = INFO

        logger.i("TAG", "some message", throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.i(throwable, "some message") }
        verify { loggerObserver.log(INFO, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun infoTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = WARN

        logger.i("TAG", "some message", throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.i(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun warningTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = WARN

        logger.w("TAG", "some message", throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.w(throwable, "some message") }
        verify { loggerObserver.log(WARN, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun warningTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = ERROR

        logger.w("TAG", "some message", throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.w(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun errorTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = ERROR

        logger.e("TAG", "some message", throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.e(throwable, "some message") }
        verify { loggerObserver.log(ERROR, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun errorTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        logger.setObserver(loggerObserver)
        logger.logLevel = NONE

        logger.e("TAG", "some message", throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.e(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToVerboseCall() {
        logger.v(null, "some message", mockk())

        verify(exactly = 0) { Timber.tag(any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToDebugCall() {
        logger.d(null, "some message", mockk())

        verify(exactly = 0) { Timber.tag(any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToInfoCall() {
        logger.i(null, "some message", mockk())

        verify(exactly = 0) { Timber.tag(any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToWarningCall() {
        logger.w(null, "some message", mockk())

        verify(exactly = 0) { Timber.tag(any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToErrorCall() {
        logger.e(null, "some message", mockk())

        verify(exactly = 0) { Timber.tag(any()) }
    }
}
