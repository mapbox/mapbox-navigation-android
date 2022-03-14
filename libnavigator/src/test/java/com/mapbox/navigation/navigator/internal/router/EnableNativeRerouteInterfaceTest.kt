package com.mapbox.navigation.navigator.internal.router

import com.mapbox.navigation.navigator.internal.customConfigEnableNativeRerouteInterface
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class EnableNativeRerouteInterfaceTest(
    private val description: String,
    private val string: String,
    private val result: String,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(
                "Empty string",
                "",
                """
                    {
                        "features": {
                            "useInternalReroute": true
                        }
                    }
                """.trimIndent()
            ),
            arrayOf(
                "Empty json",
                "{}",
                """
                    {
                        "features": {
                            "useInternalReroute": true
                        }
                    }
                """.trimIndent()
            ),
            arrayOf(
                "Other configs enabled",
                """
                    {
                        "alertsService": {
                            "enabled": true,
                            "radius": 1000.0
                        },
                        "cache": {
                            "enableAssetsTrackingMode": false
                        }
                    }
                """.trimIndent(),
                """
                    {
                        "alertsService": {
                            "enabled": true,
                            "radius": 1000.0
                        },
                        "cache": {
                            "enableAssetsTrackingMode": false
                        },
                        "features": {
                            "useInternalReroute": true
                        }
                    }
                """.trimIndent()
            ),
            arrayOf(
                "features other flags enable",
                """
                    {
                        "features": {
                            "collectMppMetrics": false,
                            "collectTeleportFallbackMetrics": false,
                            "useCommonTelemetry": false
                        }
                    }
                """.trimIndent(),
                """
                    {
                        "features": {
                            "collectMppMetrics": false,
                            "collectTeleportFallbackMetrics": false,
                            "useCommonTelemetry": false,
                            "useInternalReroute": true
                        }
                    }
                """.trimIndent()
            ),
            arrayOf(
                "features reroute disabled",
                """
                    {
                        "features": {
                            "useInternalReroute": false
                        }
                    }
                """.trimIndent(),
                """
                    {
                        "features": {
                            "useInternalReroute": true
                        }
                    }
                """.trimIndent()
            ),
            arrayOf(
                "features reroute enabled",
                """
                    {
                        "features": {
                            "useInternalReroute": true
                        }
                    }
                """.trimIndent(),
                """
                    {
                        "features": {
                            "useInternalReroute": true
                        }
                    }
                """.trimIndent()
            ),
        )
    }

    @Test
    fun testCases() {
        val result = string.customConfigEnableNativeRerouteInterface()

        assertEquals(
            description,
            JSONObject(this.result).toString(), JSONObject(result).toString()
        )
    }
}
