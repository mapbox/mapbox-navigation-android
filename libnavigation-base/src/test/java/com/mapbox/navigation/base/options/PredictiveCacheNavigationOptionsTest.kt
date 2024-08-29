package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class PredictiveCacheNavigationOptionsTest :
    BuilderTest<PredictiveCacheNavigationOptions, PredictiveCacheNavigationOptions.Builder>() {
    override fun getImplementationClass(): KClass<PredictiveCacheNavigationOptions> =
        PredictiveCacheNavigationOptions::class

    override fun getFilledUpBuilder(): PredictiveCacheNavigationOptions.Builder =
        PredictiveCacheNavigationOptions.Builder().apply {
            predictiveCacheLocationOptions(
                PredictiveCacheLocationOptions.Builder().apply {
                    currentLocationRadiusInMeters(200)
                    routeBufferRadiusInMeters(10)
                    destinationLocationRadiusInMeters(1000)
                }.build(),
            )
        }

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
