package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class TunnelInfoTest :
    BuilderTest<TunnelInfo, TunnelInfo.Builder>() {

    override fun getImplementationClass() = TunnelInfo::class

    override fun getFilledUpBuilder() = TunnelInfo.Builder(
        "Ted Williams Tunnel"
    )

    @Test
    override fun trigger() {
        // see docs
    }
}
