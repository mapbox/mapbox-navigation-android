package com.mapbox.navigation.ui.internal.utils

import com.mapbox.navigation.testing.BuilderTest
import kotlin.reflect.KClass
import org.junit.Test

class BitmapEncodeOptionsTest : BuilderTest<BitmapEncodeOptions, BitmapEncodeOptions.Builder>() {
    override fun getImplementationClass(): KClass<BitmapEncodeOptions> = BitmapEncodeOptions::class
    override fun getFilledUpBuilder(): BitmapEncodeOptions.Builder {
        return BitmapEncodeOptions.Builder()
            .compressQuality(88)
            .width(456)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
