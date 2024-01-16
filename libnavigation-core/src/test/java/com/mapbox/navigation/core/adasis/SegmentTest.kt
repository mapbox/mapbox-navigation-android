package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class SegmentTest : BuilderTest<Segment, Segment.Builder>() {

    override fun getImplementationClass() = Segment::class

    override fun getFilledUpBuilder(): Segment.Builder {
        return Segment.Builder()
            .options(AdasisConfigMessageOptions.Builder().radiusMeters(12345).build())
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
