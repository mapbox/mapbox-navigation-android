package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class PredictiveCacheOptionsTest :
    BuilderTest<PredictiveCacheOptions, PredictiveCacheOptions.Builder>() {
    override fun getImplementationClass(): KClass<PredictiveCacheOptions> =
        PredictiveCacheOptions::class

    override fun getFilledUpBuilder(): PredictiveCacheOptions.Builder =
        PredictiveCacheOptions.Builder().apply {
            predictiveCacheNavigationOptions(
                PredictiveCacheNavigationOptions.Builder().apply {
                    predictiveCacheLocationOptions(
                        PredictiveCacheLocationOptions.Builder().apply {
                            currentLocationRadiusInMeters(300)
                            routeBufferRadiusInMeters(50)
                            destinationLocationRadiusInMeters(20)
                        }.build()
                    )
                }.build()
            )
            predictiveCacheMapsOptions(
                PredictiveCacheMapsOptions.Builder().apply {
                    predictiveCacheLocationOptions(
                        PredictiveCacheLocationOptions.Builder().apply {
                            currentLocationRadiusInMeters(100)
                            routeBufferRadiusInMeters(2)
                            destinationLocationRadiusInMeters(15)
                        }.build()
                    )
                    minZoom(1)
                    maxZoom(3)
                }.build()
            )
        }

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
