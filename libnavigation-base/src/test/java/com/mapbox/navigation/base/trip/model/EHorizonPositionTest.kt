package com.mapbox.navigation.base.trip.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class EHorizonPositionTest : BuilderTest<EHorizonPosition, EHorizonPosition.Builder>() {

    override fun getImplementationClass() = EHorizonPosition::class

    override fun getFilledUpBuilder() = EHorizonPosition.Builder()
        .edgeId(3)
        .percentAlong(25.0)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
