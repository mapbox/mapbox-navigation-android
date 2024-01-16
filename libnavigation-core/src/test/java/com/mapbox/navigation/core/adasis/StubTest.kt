package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class StubTest : BuilderTest<Stub, Stub.Builder>() {

    override fun getImplementationClass() = Stub::class

    override fun getFilledUpBuilder(): Stub.Builder {
        return Stub.Builder()
            .options(AdasisConfigMessageOptions.Builder().radiusMeters(12345).build())
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
