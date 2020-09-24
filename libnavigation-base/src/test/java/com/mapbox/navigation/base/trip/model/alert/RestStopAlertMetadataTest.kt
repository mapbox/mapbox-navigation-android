package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class RestStopAlertMetadataTest :
    BuilderTest<RestStopAlert.Metadata, RestStopAlert.Metadata.Builder>() {
    override fun getImplementationClass() = RestStopAlert.Metadata::class

    override fun getFilledUpBuilder() =
        RestStopAlert.Metadata.Builder().type(RestStopType.RestArea)

    @Test
    override fun trigger() {
        // see doc
    }
}
