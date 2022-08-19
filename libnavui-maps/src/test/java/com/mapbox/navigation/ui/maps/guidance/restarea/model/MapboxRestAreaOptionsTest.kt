package com.mapbox.navigation.ui.maps.guidance.restarea.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class MapboxRestAreaOptionsTest :
    BuilderTest<MapboxRestAreaOptions, MapboxRestAreaOptions.Builder>() {

    override fun getImplementationClass(): KClass<MapboxRestAreaOptions> =
        MapboxRestAreaOptions::class

    override fun getFilledUpBuilder(): MapboxRestAreaOptions.Builder {
        val mockDesiredWidth = 200
        return MapboxRestAreaOptions.Builder()
            .desiredGuideMapWidth(mockDesiredWidth)
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
