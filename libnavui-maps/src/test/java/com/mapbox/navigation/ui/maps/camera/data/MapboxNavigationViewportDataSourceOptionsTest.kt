package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class MapboxNavigationViewportDataSourceOptionsTest :
    BuilderTest<MapboxNavigationViewportDataSourceOptions,
        MapboxNavigationViewportDataSourceOptions.Builder>() {
    override fun getImplementationClass() = MapboxNavigationViewportDataSourceOptions::class

    override fun getFilledUpBuilder() = MapboxNavigationViewportDataSourceOptions.Builder()
        .maxFollowingPitch(12.3)
        .minFollowingZoom(14.213)
        .maxZoom(45.6)

    @Test
    override fun trigger() {
        // see docs
    }
}
