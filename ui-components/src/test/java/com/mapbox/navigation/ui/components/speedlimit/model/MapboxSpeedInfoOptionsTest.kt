package com.mapbox.navigation.ui.components.speedlimit.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import kotlin.reflect.KClass

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxSpeedInfoOptionsTest :
    BuilderTest<MapboxSpeedInfoOptions, MapboxSpeedInfoOptions.Builder>() {
    override fun getImplementationClass(): KClass<MapboxSpeedInfoOptions> =
        MapboxSpeedInfoOptions::class

    override fun getFilledUpBuilder(): MapboxSpeedInfoOptions.Builder {
        return MapboxSpeedInfoOptions
            .Builder()
            .showLegend(true)
            .showUnit(false)
            .speedInfoStyle(mockk())
            .showSpeedWhenUnavailable(true)
            .renderWithSpeedSign(SpeedLimitSign.VIENNA)
            .currentSpeedDirection(CurrentSpeedDirection.END)
    }

    override fun trigger() {
        // see comments
    }
}
