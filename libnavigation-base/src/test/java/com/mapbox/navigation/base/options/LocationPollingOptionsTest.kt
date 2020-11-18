package com.mapbox.navigation.base.options

import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class LocationPollingOptionsTest :
    BuilderTest<LocationPollingOptions, LocationPollingOptions.Builder>() {

    override fun getImplementationClass() = LocationPollingOptions::class

    override fun getFilledUpBuilder() = LocationPollingOptions.Builder()
        .navigatorIntervalMillis(4500)
        .navigatorPatienceMillis(500)
        .locationEngineRequest(
            LocationEngineRequest.Builder(4000)
                .setPriority(LocationEngineRequest.PRIORITY_LOW_POWER)
                .setFastestInterval(2000)
                .build()
        )

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
