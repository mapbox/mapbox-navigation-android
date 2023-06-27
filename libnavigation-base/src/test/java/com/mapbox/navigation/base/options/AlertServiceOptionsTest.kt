package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class AlertServiceOptionsTest : BuilderTest<AlertServiceOptions, AlertServiceOptions.Builder>() {

    override fun getImplementationClass() = AlertServiceOptions::class

    override fun getFilledUpBuilder() = AlertServiceOptions.Builder()
        .collectTunnels(false)
        .collectBridges(false)
        .collectRestrictedAreas(true)
        .collectMergingAreas(true)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
