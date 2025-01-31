package com.mapbox.navigation.base.options

import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class PredictiveCacheSearchOptionsTest :
    BuilderTest<PredictiveCacheSearchOptions,
        PredictiveCacheSearchOptions.Builder,>() {

    override fun getImplementationClass() =
        PredictiveCacheSearchOptions::class

    override fun getFilledUpBuilder(): PredictiveCacheSearchOptions.Builder =
        PredictiveCacheSearchOptions.Builder(mockTilesetDescriptor).apply {
            predictiveCacheLocationOptions(
                PredictiveCacheLocationOptions.Builder().apply {
                    currentLocationRadiusInMeters(400)
                    routeBufferRadiusInMeters(4)
                    destinationLocationRadiusInMeters(21)
                }.build(),
            )
        }

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    private val mockTilesetDescriptor: TilesetDescriptor = mockk()
}
