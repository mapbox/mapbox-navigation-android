package com.mapbox.navigation.ui.speedlimit

import android.content.Context
import androidx.annotation.StyleRes
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.speedlimit.internal.SpeedInfoComponent
import com.mapbox.navigation.ui.speedlimit.internal.SpeedLimitComponent
import com.mapbox.navigation.ui.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedInfoView
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView

/**
 * Install components that render [MapboxSpeedLimitView].
 *
 * @param speedLimitView [MapboxSpeedLimitView]
 * @param config SpeedLimitConfig
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
 * Install components that render [MapboxSpeedInfoView].
 *
 * @param speedInfoView [MapboxSpeedInfoView]
 * @param config SpeedInfoConfig
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.speedInfo(
    speedInfoView: MapboxSpeedInfoView,
    config: SpeedInfoConfig.() -> Unit = {}
): Installation {
    val componentConfig = SpeedInfoConfig(speedInfoView.context).apply(config)
    return component(
        SpeedInfoComponent(
            speedInfoOptions = componentConfig.speedInfoOptions,
            speedInfoView = speedInfoView,
            distanceFormatterOptions = componentConfig.distanceFormatterOptions
        )
    )
}

/**
 * Speed limit view component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
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

/**
 * Speed info view component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class SpeedInfoConfig internal constructor(context: Context) {
    /**
     * [speedInfoOptions] to be set to [MapboxSpeedInfoView].
     */
    var speedInfoOptions: MapboxSpeedInfoOptions = MapboxSpeedInfoOptions.Builder().build()

    /**
     * [DistanceFormatterOptions] to be used by [MapboxSpeedInfoView].
     */
    var distanceFormatterOptions: DistanceFormatterOptions =
        DistanceFormatterOptions.Builder(context).build()
}
