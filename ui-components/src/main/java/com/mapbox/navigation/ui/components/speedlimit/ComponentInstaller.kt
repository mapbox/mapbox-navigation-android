package com.mapbox.navigation.ui.components.speedlimit

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.components.speedlimit.internal.SpeedInfoComponent
import com.mapbox.navigation.ui.components.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.components.speedlimit.view.MapboxSpeedInfoView

/**
 * Install components that render [MapboxSpeedInfoView].
 *
 * @param speedInfoView [MapboxSpeedInfoView]
 * @param config SpeedInfoConfig
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.speedInfo(
    speedInfoView: MapboxSpeedInfoView,
    config: SpeedInfoConfig.() -> Unit = {},
): Installation {
    val componentConfig = SpeedInfoConfig(speedInfoView.context).apply(config)
    return component(
        SpeedInfoComponent(
            speedInfoOptions = componentConfig.speedInfoOptions,
            speedInfoView = speedInfoView,
            distanceFormatterOptions = componentConfig.distanceFormatterOptions,
        ),
    )
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
