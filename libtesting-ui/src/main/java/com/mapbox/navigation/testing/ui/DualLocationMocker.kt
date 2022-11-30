package com.mapbox.navigation.testing.ui

import android.location.Location

/**
 * Mocker that pushes updates to both location provider implementations
 * so that the sample is available regardless where the running location engine tries to get it from.
 *
 * Introduction of this dual mocker was needed to initially work around CORESDK-1528
 * so that the last location is available in both Google fused and Android location providers at the same time.
 */
internal class DualLocationMocker(
    private val systemLocationMocker: SystemLocationMocker,
    private val fusedLocationMocker: FusedLocationMocker?,
) : LocationMocker {
    override fun before() {
        systemLocationMocker.before()
        fusedLocationMocker?.before()
    }

    override fun after() {
        systemLocationMocker.after()
        fusedLocationMocker?.after()
    }

    override fun mockLocation(location: Location) {
        systemLocationMocker.mockLocation(location)
        fusedLocationMocker?.mockLocation(
            location.apply {
                provider = FusedLocationMocker.DEFAULT_PROVIDER_NAME
            }
        )
    }

    override fun generateDefaultLocation(): Location {
        return systemLocationMocker.generateDefaultLocation()
    }
}
