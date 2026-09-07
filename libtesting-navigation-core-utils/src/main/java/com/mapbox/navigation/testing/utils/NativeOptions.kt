package com.mapbox.navigation.testing.utils


fun getTestRerouteCustomConfig(): String = stableNativeSimulationConfig

private const val stableNativeSimulationConfig =
    """
        "router": {
                "hybridRouterConfig": {
                    "fallbackDelaySeconds": 0,
                    "timeoutToFallbackSeconds": 1
                }
            },
        "input": {
                "extrapolation": {
                    "mode": 0
                }
            },
        "navigation": {
                "noSignalSimulation": {
                    "enabled": false
                }
            }
    """

