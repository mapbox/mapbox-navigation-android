package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class PredictiveCacheMapsOptionsTest :
    BuilderTest<PredictiveCacheMapsOptions, PredictiveCacheMapsOptions.Builder>() {

    override fun getImplementationClass(): KClass<PredictiveCacheMapsOptions> =
        PredictiveCacheMapsOptions::class

    override fun getFilledUpBuilder(): PredictiveCacheMapsOptions.Builder =
        PredictiveCacheMapsOptions.Builder().apply {
            predictiveCacheLocationOptions(
                PredictiveCacheLocationOptions.Builder().apply {
                    currentLocationRadiusInMeters(300)
                    routeBufferRadiusInMeters(50)
                    destinationLocationRadiusInMeters(20)
                }.build(),
            )
            minZoom(20)
            maxZoom(30)
            extraOptions(mockk(relaxed = true))
        }

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
