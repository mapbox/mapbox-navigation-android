package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class TunnelEntranceAlertMetadataTest :
    BuilderTest<TunnelEntranceAlert.Metadata, TunnelEntranceAlert.Metadata.Builder>() {
    override fun getImplementationClass() = TunnelEntranceAlert.Metadata::class

    override fun getFilledUpBuilder() = TunnelEntranceAlert.Metadata.Builder()
        .tunnelName("tunnelName")

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
