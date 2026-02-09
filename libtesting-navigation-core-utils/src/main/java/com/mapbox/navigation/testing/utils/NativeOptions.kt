package com.mapbox.navigation.testing.utils

const val nativeRerouteControllerNoRetryConfig =
    """{
            "features": {
                "useInternalReroute": true
            },
            "navigation": {
                "offRoadDetector":{
                    "reroute": {
                        "doNotRetryRerouteOnFailure": true
                    }
                }
            }
        }"""
