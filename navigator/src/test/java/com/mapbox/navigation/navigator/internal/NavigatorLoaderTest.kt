package com.mapbox.navigation.navigator.internal

import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigatorLoaderTest {

    private val loggingFrontend = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerFrontendTestRule = LoggingFrontendTestRule(loggingFrontend)

    @Test
    fun `test enable telemetry immediate`() {
        val adjustedConfig = NavigatorLoader.enableTelemetryNavigationEvents(
            DEFAULT_CONFIG,
            sendImmediate = true,
        )

        assertEqualsJson(DEFAULT_CONFIG_USE_TELEMETRY_IMMEDIATE, adjustedConfig)
    }

    @Test
    fun `test enable telemetry not-immediate`() {
        val adjustedConfig = NavigatorLoader.enableTelemetryNavigationEvents(
            DEFAULT_CONFIG,
            sendImmediate = false,
        )

        assertEqualsJson(DEFAULT_CONFIG_USE_TELEMETRY, adjustedConfig)
    }

    @Test
    fun `test enable telemetry on empty config string`() {
        val adjustedConfig = NavigatorLoader.enableTelemetryNavigationEvents(
            "",
            sendImmediate = false,
        )
        assertEqualsJson(DEFAULT_CONFIG_USE_TELEMETRY, adjustedConfig)
    }

    @Test
    fun `test enable telemetry on incorrect config string`() {
        val adjustedConfig = NavigatorLoader.enableTelemetryNavigationEvents(
            "incorrect-json-string",
            sendImmediate = false,
        )
        assertEqualsJson(DEFAULT_CONFIG_USE_TELEMETRY, adjustedConfig)
        verify(exactly = 1) {
            loggingFrontend.logE(any())
        }
    }

    @Test
    fun `appends required data to existing config`() {
        val defaultConfig = """
            {
            "foo": "bar"
            }
        """

        val expectedConfig = """
            {
                "foo": "bar",
                "features": {
                    "useTelemetryNavigationEvents": true
                }
            }
        """

        val fixedConfig = NavigatorLoader.enableTelemetryNavigationEvents(
            defaultConfig,
            sendImmediate = false,
        )

        assertEqualsJson(expectedConfig, fixedConfig)
    }

    private companion object {

        const val DEFAULT_CONFIG = "{}"

        const val DEFAULT_CONFIG_USE_TELEMETRY = """
            {
                "features": {
                    "useTelemetryNavigationEvents": true
                }
            }
        """

        const val DEFAULT_CONFIG_USE_TELEMETRY_IMMEDIATE = """
            {
                "features": {
                    "useTelemetryNavigationEvents": true
                },
                "telemetry": {
                    "eventsPriority": "Immediate"
                }
            }
        """

        fun assertEqualsJson(expected: String, actual: String) {
            assertEquals(JSONObject(expected).toString(), JSONObject(actual).toString())
        }
    }
}
