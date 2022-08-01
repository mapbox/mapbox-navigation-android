package com.mapbox.navigation.ui.speedlimit

import androidx.annotation.StyleRes
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.speedlimit.internal.SpeedLimitComponent
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView

/**
 * Install components that render [MapboxSpeedLimitView].
 *
 * @param style definition for [MapboxSpeedLimitView]
 * @param textAppearance for [MapboxSpeedLimitView]
 * @param speedLimitView [MapboxSpeedLimitView]
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.speedLimit(
    speedLimitView: MapboxSpeedLimitView,
    config: SpeedLimitConfig.() -> Unit = {}
): Installation {
    val componentConfig = SpeedLimitConfig().apply(config)
    return component(
        SpeedLimitComponent(
            style = componentConfig.style,
            textAppearance = componentConfig.textAppearance,
            speedLimitView = speedLimitView
        )
    )
}

/**
 * Speed limit view component configuration class.
 */
class SpeedLimitConfig internal constructor() {
    /**
     * [style] to be set to [MapboxSpeedLimitView].
     */
    @StyleRes var style: Int = R.style.MapboxStyleSpeedLimit
    /**
     * [textAppearance] to be set to [MapboxSpeedLimitView].
     */
    @StyleRes var textAppearance: Int = 0
}
