package com.mapbox.navigation.utils.internal

import com.mapbox.common.LoggingLevel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LoggerProviderLazyMessagesTest(val parameter: LogData) {

    private val originalLogger = LoggerProvider.getLoggerFrontend()
    private val mockLogger = mockk<LoggerFrontend>(relaxed = true)

    @Before
    fun setup() {
        LoggerProvider.setLoggerFrontend(mockLogger)
    }

    @After
    fun tearDown() {
        LoggerProvider.setLoggerFrontend(originalLogger)
    }

    data class LogData(
        val frontendLogLevel: LoggingLevel,
        val messageLogLevel: LoggingLevel,
        val logDispatched: Boolean,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters() = listOf(
            LogData(
                frontendLogLevel = LoggingLevel.DEBUG,
                messageLogLevel = LoggingLevel.DEBUG,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.DEBUG,
                messageLogLevel = LoggingLevel.INFO,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.DEBUG,
                messageLogLevel = LoggingLevel.WARNING,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.DEBUG,
                messageLogLevel = LoggingLevel.ERROR,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.INFO,
                messageLogLevel = LoggingLevel.DEBUG,
                logDispatched = false,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.INFO,
                messageLogLevel = LoggingLevel.INFO,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.INFO,
                messageLogLevel = LoggingLevel.WARNING,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.INFO,
                messageLogLevel = LoggingLevel.ERROR,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.WARNING,
                messageLogLevel = LoggingLevel.DEBUG,
                logDispatched = false,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.WARNING,
                messageLogLevel = LoggingLevel.INFO,
                logDispatched = false,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.WARNING,
                messageLogLevel = LoggingLevel.WARNING,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.WARNING,
                messageLogLevel = LoggingLevel.ERROR,
                logDispatched = true,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.ERROR,
                messageLogLevel = LoggingLevel.DEBUG,
                logDispatched = false,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.ERROR,
                messageLogLevel = LoggingLevel.INFO,
                logDispatched = false,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.ERROR,
                messageLogLevel = LoggingLevel.WARNING,
                logDispatched = false,
            ),
            LogData(
                frontendLogLevel = LoggingLevel.ERROR,
                messageLogLevel = LoggingLevel.ERROR,
                logDispatched = true,
            ),
        )
    }

    @Test
    fun `test lazy message`() {
        every { mockLogger.getLogLevel() } returns parameter.frontendLogLevel
        when (parameter.messageLogLevel) {
            LoggingLevel.DEBUG -> logD("category") {
                "msg"
            }
            LoggingLevel.INFO -> logI("category") {
                "msg"
            }
            LoggingLevel.WARNING -> logW("category") {
                "msg"
            }
            LoggingLevel.ERROR -> logE("category") {
                "msg"
            }
        }
        if (parameter.logDispatched) {
            when (parameter.messageLogLevel) {
                LoggingLevel.DEBUG -> verify(exactly = 1) { mockLogger.logD("msg", "category") }
                LoggingLevel.INFO -> verify(exactly = 1) { mockLogger.logI("msg", "category") }
                LoggingLevel.WARNING -> verify(exactly = 1) { mockLogger.logW("msg", "category") }
                LoggingLevel.ERROR -> verify(exactly = 1) { mockLogger.logE("msg", "category") }
            }
        } else {
            verify(exactly = 0) { mockLogger.logD(any(), any()) }
            verify(exactly = 0) { mockLogger.logI(any(), any()) }
            verify(exactly = 0) { mockLogger.logW(any(), any()) }
            verify(exactly = 0) { mockLogger.logE(any(), any()) }
        }
    }
}
