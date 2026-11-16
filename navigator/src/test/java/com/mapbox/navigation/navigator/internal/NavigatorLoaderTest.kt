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
    fun `adds required features with immediate telemetry`() {
        val adjustedConfig = with(NavigatorLoader) {
            DEFAULT_CONFIG.enrichWithRequiredSdkFeatures(sendImmediate = true)
        }

        assertEqualsJson(DEFAULT_CONFIG_REQUIRED_FEATURES_IMMEDIATE, adjustedConfig)
    }

    @Test
    fun `adds required features without immediate telemetry`() {
        val adjustedConfig = with(NavigatorLoader) {
            DEFAULT_CONFIG.enrichWithRequiredSdkFeatures(sendImmediate = false)
        }

        assertEqualsJson(DEFAULT_CONFIG_REQUIRED_FEATURES, adjustedConfig)
    }

    @Test
    fun `adds required features to empty config string`() {
        val adjustedConfig = with(NavigatorLoader) {
            "".enrichWithRequiredSdkFeatures(sendImmediate = false)
        }
        assertEqualsJson(DEFAULT_CONFIG_REQUIRED_FEATURES, adjustedConfig)
    }

    @Test
    fun `adds required features to incorrect config string`() {
        val adjustedConfig = with(NavigatorLoader) {
            "incorrect-json-string".enrichWithRequiredSdkFeatures(sendImmediate = false)
        }
        assertEqualsJson(DEFAULT_CONFIG_REQUIRED_FEATURES, adjustedConfig)
        verify(exactly = 1) {
            loggingFrontend.logE(any())
        }
    }

    @Test
    fun `appends required features to existing config`() {
        val customConfig = """
            {
                "foo": "bar",
                "features": {
                    "customFeature": true
                }
            }
        """

        val expectedConfig = """
            {
                "foo": "bar",
                "features": {
                    "customFeature": true,
                    "useTelemetryNavigationEvents": true,
                    "allowTilesCacheReset": true
                }
            }
        """

        val adjustedConfig = with(NavigatorLoader) {
            customConfig.enrichWithRequiredSdkFeatures(sendImmediate = false)
        }

        assertEqualsJson(expectedConfig, adjustedConfig)
    }

    private companion object {

        const val DEFAULT_CONFIG = "{}"

        const val DEFAULT_CONFIG_REQUIRED_FEATURES = """
            {
                "features": {
                    "useTelemetryNavigationEvents": true,
                    "allowTilesCacheReset": true
                }
            }
        """

        const val DEFAULT_CONFIG_REQUIRED_FEATURES_IMMEDIATE = """
            {
                "features": {
                    "useTelemetryNavigationEvents": true,
                    "allowTilesCacheReset": true
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
