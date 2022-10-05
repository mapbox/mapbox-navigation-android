package com.mapbox.navigation.utils.internal

import com.mapbox.common.LoggingLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LoggingLevelUtilKtTest(val parameter: LogLevelData) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters() = listOf(
            LogLevelData(
                what = null,
                atLeast = LoggingLevel.ERROR,
                expected = false
            ),
            LogLevelData(
                what = null,
                atLeast = LoggingLevel.WARNING,
                expected = false
            ),
            LogLevelData(
                what = null,
                atLeast = LoggingLevel.INFO,
                expected = false
            ),
            LogLevelData(
                what = null,
                atLeast = LoggingLevel.DEBUG,
                expected = false
            ),

            LogLevelData(
                what = LoggingLevel.ERROR,
                atLeast = LoggingLevel.ERROR,
                expected = true
            ),
            LogLevelData(
                what = LoggingLevel.ERROR,
                atLeast = LoggingLevel.WARNING,
                expected = true
            ),
            LogLevelData(
                what = LoggingLevel.ERROR,
                atLeast = LoggingLevel.INFO,
                expected = true
            ),
            LogLevelData(
                what = LoggingLevel.ERROR,
                atLeast = LoggingLevel.DEBUG,
                expected = true
            ),

            LogLevelData(
                what = LoggingLevel.WARNING,
                atLeast = LoggingLevel.ERROR,
                expected = false
            ),
            LogLevelData(
                what = LoggingLevel.WARNING,
                atLeast = LoggingLevel.WARNING,
                expected = true
            ),
            LogLevelData(
                what = LoggingLevel.ERROR,
                atLeast = LoggingLevel.INFO,
                expected = true
            ),
            LogLevelData(
                what = LoggingLevel.ERROR,
                atLeast = LoggingLevel.DEBUG,
                expected = true
            ),

            LogLevelData(
                what = LoggingLevel.INFO,
                atLeast = LoggingLevel.ERROR,
                expected = false
            ),
            LogLevelData(
                what = LoggingLevel.INFO,
                atLeast = LoggingLevel.WARNING,
                expected = false
            ),
            LogLevelData(
                what = LoggingLevel.INFO,
                atLeast = LoggingLevel.INFO,
                expected = true
            ),
            LogLevelData(
                what = LoggingLevel.INFO,
                atLeast = LoggingLevel.DEBUG,
                expected = true
            ),

            LogLevelData(
                what = LoggingLevel.DEBUG,
                atLeast = LoggingLevel.ERROR,
                expected = false
            ),
            LogLevelData(
                what = LoggingLevel.DEBUG,
                atLeast = LoggingLevel.WARNING,
                expected = false
            ),
            LogLevelData(
                what = LoggingLevel.DEBUG,
                atLeast = LoggingLevel.INFO,
                expected = false
            ),
            LogLevelData(
                what = LoggingLevel.DEBUG,
                atLeast = LoggingLevel.DEBUG,
                expected = true
            ),
        )
    }

    @Test
    fun `test atLeast`() {
        val result = parameter.what.atLeast(parameter.atLeast)
        assertEquals(parameter.expected, result)
    }

    data class LogLevelData(
        val what: LoggingLevel?,
        val atLeast: LoggingLevel,
        val expected: Boolean
    )
}