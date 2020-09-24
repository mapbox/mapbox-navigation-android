package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class TunnelEntranceAlertMetadataTest :
    BuilderTest<TunnelEntranceAlert.Metadata, TunnelEntranceAlert.Metadata.Builder>() {
    override fun getImplementationClass() = TunnelEntranceAlert.Metadata::class

    override fun getFilledUpBuilder() = TunnelEntranceAlert.Metadata.Builder()

    @Test
    override fun trigger() {
        // see docs
    }
}
