package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class TollCollectionAlertMetadataTest :
    BuilderTest<TollCollectionAlert.Metadata, TollCollectionAlert.Metadata.Builder>() {
    override fun getImplementationClass() = TollCollectionAlert.Metadata::class

    override fun getFilledUpBuilder() =
        TollCollectionAlert.Metadata.Builder().type(TollCollectionType.TollGantry)

    @Test
    override fun trigger() {
        // see doc
    }
}
