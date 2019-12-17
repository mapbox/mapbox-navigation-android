package com.mapbox.navigation.logger

import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class MapboxLoggerTest {

    @Before
    fun setUp() {
        mockkStatic(Timber::class)
    }

    @Test
    fun verboseTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = VERBOSE

        MapboxLogger.v(Tag("TAG"), Message("some message"), throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.v(throwable, "some message") }
        verify { loggerObserver.log(VERBOSE, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun verboseTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = DEBUG

        MapboxLogger.v(Tag("TAG"), Message("some message"), throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.v(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun debugTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = DEBUG

        MapboxLogger.d(Tag("TAG"), Message("some message"), throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.d(throwable, "some message") }
        verify { loggerObserver.log(DEBUG, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun debugTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = INFO

        MapboxLogger.d(Tag("TAG"), Message("some message"), throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.d(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun infoTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = INFO

        MapboxLogger.i(Tag("TAG"), Message("some message"), throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.i(throwable, "some message") }
        verify { loggerObserver.log(INFO, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun infoTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = WARN

        MapboxLogger.i(Tag("TAG"), Message("some message"), throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.i(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun warningTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = WARN

        MapboxLogger.w(Tag("TAG"), Message("some message"), throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.w(throwable, "some message") }
        verify { loggerObserver.log(WARN, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun warningTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = ERROR

        MapboxLogger.w(Tag("TAG"), Message("some message"), throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.w(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun errorTestSuccessful() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = ERROR

        MapboxLogger.e(Tag("TAG"), Message("some message"), throwable)

        verify { Timber.tag("TAG") }
        verify { Timber.e(throwable, "some message") }
        verify { loggerObserver.log(ERROR, LogEntry("TAG", "some message", throwable)) }
    }

    @Test
    fun errorTestIncorrectLogLevel() {
        val throwable = mockk<Throwable>()
        val loggerObserver = mockk<LoggerObserver>(relaxed = true)
        MapboxLogger.setObserver(loggerObserver)
        MapboxLogger.logLevel = NONE

        MapboxLogger.e(Tag("TAG"), Message("some message"), throwable)

        verify(exactly = 0) { Timber.tag(any()) }
        verify(exactly = 0) { Timber.e(any<Throwable>(), any()) }
        verify(exactly = 0) { loggerObserver.log(any(), any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToVerboseCall() {
        MapboxLogger.v(Message("some message"))

        verify(exactly = 0) { Timber.tag(any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToDebugCall() {
        MapboxLogger.d(Message("some message"))

        verify(exactly = 0) { Timber.tag(any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToInfoCall() {
        MapboxLogger.i(Message("some message"))

        verify(exactly = 0) { Timber.tag(any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToWarningCall() {
        MapboxLogger.w(Message("some message"))

        verify(exactly = 0) { Timber.tag(any()) }
    }

    @Test
    fun tagSetupNotCalledWhenNullTagPassedToErrorCall() {
        MapboxLogger.e(Message("some message"))

        verify(exactly = 0) { Timber.tag(any()) }
    }
}
