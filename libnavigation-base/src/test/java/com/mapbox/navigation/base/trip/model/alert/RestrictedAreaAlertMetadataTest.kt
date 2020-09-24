package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class RestrictedAreaAlertMetadataTest :
    BuilderTest<RestrictedAreaAlert.Metadata, RestrictedAreaAlert.Metadata.Builder>() {
    override fun getImplementationClass() = RestrictedAreaAlert.Metadata::class

    override fun getFilledUpBuilder() = RestrictedAreaAlert.Metadata.Builder()

    @Test
    override fun trigger() {
        // see docs
    }
}
