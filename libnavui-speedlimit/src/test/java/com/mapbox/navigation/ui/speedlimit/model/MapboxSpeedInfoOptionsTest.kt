package com.mapbox.navigation.ui.speedlimit.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.testing.BuilderTest
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
            .showSpeedWhenUnavailable(true)
            .renderWithSpeedSign(SpeedLimitSign.VIENNA)
            .currentSpeedDirection(CurrentSpeedDirection.END)
    }

    override fun trigger() {
        // see comments
    }
}
