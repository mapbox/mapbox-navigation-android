package com.mapbox.navigation.core.telemetry.events

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class BitmapEncodeOptionsTest : BuilderTest<BitmapEncodeOptions, BitmapEncodeOptions.Builder>() {

    override fun getImplementationClass() = BitmapEncodeOptions::class

    override fun getFilledUpBuilder(): BitmapEncodeOptions.Builder {
        return BitmapEncodeOptions.Builder()
            .width(100)
            .compressQuality(42)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
