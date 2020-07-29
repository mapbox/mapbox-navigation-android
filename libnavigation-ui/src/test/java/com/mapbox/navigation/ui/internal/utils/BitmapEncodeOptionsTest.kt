package com.mapbox.navigation.ui.internal.utils

import com.mapbox.navigation.testing.BuilderTest
import kotlin.reflect.KClass

class BitmapEncodeOptionsTest : BuilderTest<BitmapEncodeOptions, BitmapEncodeOptions.Builder>() {
    override fun getImplementationClass(): KClass<BitmapEncodeOptions> = BitmapEncodeOptions::class
    override fun getFilledUpBuilder(): BitmapEncodeOptions.Builder {
        return BitmapEncodeOptions.Builder()
            .compressQuality(88)
            .width(456)
    }
}
