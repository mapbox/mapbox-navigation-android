package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class BorderCrossingAlertMetadataTest :
    BuilderTest<BorderCrossingAlert.Metadata, BorderCrossingAlert.Metadata.Builder>() {
    override fun getImplementationClass() = BorderCrossingAlert.Metadata::class

    override fun getFilledUpBuilder() = BorderCrossingAlert.Metadata.Builder()
        .from(BorderCrossingAdminInfo.Builder("1", "2").build())
        .to(BorderCrossingAdminInfo.Builder("3", "4").build())

    @Test
    override fun trigger() {
        // see parent doc
    }
}
