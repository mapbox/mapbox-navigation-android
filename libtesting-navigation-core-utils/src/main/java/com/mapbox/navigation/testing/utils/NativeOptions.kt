package com.mapbox.navigation.testing.utils

const val nativeRerouteControllerNoRetryConfig =
    """{
            "features": {
                "useInternalReroute": true
            },
            "navigation": {
                "reroute": {
                    "doNotCancelRerouteOnBackToRoute": true,
                    "doNotRetryRerouteOnFailure": true
                }
            }
        }"""
