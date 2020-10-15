package com.mapbox.navigation.base.trip.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class NameInfoTest : BuilderTest<NameInfo, NameInfo.Builder>() {

    override fun getImplementationClass() = NameInfo::class

    override fun getFilledUpBuilder() = NameInfo.Builder()
        .name("King Farm Blvd")
        .shielded(true)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
