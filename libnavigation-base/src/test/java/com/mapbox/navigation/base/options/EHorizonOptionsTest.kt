package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class EHorizonOptionsTest : BuilderTest<EHorizonOptions, EHorizonOptions.Builder>() {

    override fun getImplementationClass() = EHorizonOptions::class

    override fun getFilledUpBuilder() = EHorizonOptions.Builder()
        .length(1500.0)
        .expansion(1)
        .branchLength(150.0)
        .includeGeometries(true)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
