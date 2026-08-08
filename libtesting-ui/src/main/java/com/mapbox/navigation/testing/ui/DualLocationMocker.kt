package com.mapbox.navigation.testing.ui

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.mapbox.navigation.testing.ui.utils.executeShellCommandBlocking

/**
 * Mocker that pushes updates to both location provider implementations
 * so that the sample is available regardless where the running location engine tries to get it from.
 *
 * Introduction of this dual mocker was needed to initially work around CORESDK-1528
 * so that the last location is available in both Google fused and Android location providers at the same time.
 */
internal class DualLocationMocker(
    private val context: Context,
    private val systemLocationMocker: SystemLocationMocker,
    private val fusedLocationMocker: FusedLocationMocker?,
) : LocationMocker {

    private val instrumentation = getInstrumentation()

    override fun before() {
        instrumentation.uiAutomation.executeShellCommandBlocking(
            "appops set " +
                context.packageName +
                " android:mock_location allow"
        )

        systemLocationMocker.before()
        fusedLocationMocker?.before()
    }

    override fun after() {
        systemLocationMocker.after()
        fusedLocationMocker?.after()
    }

    override fun mockLocation(location: Location) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            "MockLocationUpdatesRule is supported only on Android devices " +
                "running version >= Build.VERSION_CODES.M"
        }

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
