package com.mapbox.navigation.testing.utils

private const val stableNativeSimulationConfigInternal =
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

const val stableNativeSimulationConfig =
    """{
            $stableNativeSimulationConfigInternal
        }"""

const val nativeRerouteControllerNoRetryConfig =
    """{
            "features": {
                "useInternalReroute": true
            },
            $stableNativeSimulationConfigInternal
        }"""
