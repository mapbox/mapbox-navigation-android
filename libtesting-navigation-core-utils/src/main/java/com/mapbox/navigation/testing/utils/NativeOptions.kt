package com.mapbox.navigation.testing.utils


fun getTestRerouteCustomConfig(nativeReroute: Boolean): String = if (nativeReroute) {
    stableNativeSimulationConfig
} else {
    platformRerouteConfig
}

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

private const val platformRerouteConfig =
    """{
            "features": {
                "useInternalReroute": false
            },
            $stableNativeSimulationConfig
        }"""

