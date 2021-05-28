package com.mapbox.navigation.ui.maps.guidance.signboard.model

import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(NavSDKRobolectricTestRunner::class)
class MapboxSignboardOptionsTest : BuilderTest<MapboxSignboardOptions,
    MapboxSignboardOptions.Builder>() {

    override fun getImplementationClass(): KClass<MapboxSignboardOptions> =
        MapboxSignboardOptions::class

    override fun getFilledUpBuilder(): MapboxSignboardOptions.Builder {
        val mockDesiredWidth = 200
        val mockCssStyles = "whatever"
        return MapboxSignboardOptions.Builder()
            .desiredSignboardWidth(mockDesiredWidth)
            .cssStyles(mockCssStyles)
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
