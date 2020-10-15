package com.mapbox.navigation.base.trip.model

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class EHorizonTest : BuilderTest<EHorizon, EHorizon.Builder>() {

    override fun getImplementationClass() = EHorizon::class

    override fun getFilledUpBuilder() = EHorizon.Builder()
        .start(mockk())

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
