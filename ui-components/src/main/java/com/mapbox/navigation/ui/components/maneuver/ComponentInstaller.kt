package com.mapbox.navigation.ui.components.maneuver

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.maneuver.internal.ManeuverComponent
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverView

/**
 * Install components that render [MapboxManeuverView].
 *
 * @param maneuverView MapboxManeuverView
 * @param config options that can be used to configure [ManeuverViewOptions]
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.maneuver(
    maneuverView: MapboxManeuverView,
    config: ManeuverConfig.() -> Unit = {},
): Installation {
    val componentConfig = ManeuverConfig(maneuverView.context).apply(config)
    return component(
        ManeuverComponent(
            maneuverView = maneuverView,
            userId = componentConfig.userId,
            styleId = componentConfig.styleId,
            componentConfig.options,
            componentConfig.distanceFormatterOptions,
        ),
    )
}

/**
 * Maneuver view component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ManeuverConfig internal constructor(context: Context) {
    /**
     * [userId]
     */
    var userId: String? = null

    /**
     * [styleId] used by maps style.
     */
    var styleId: String? = null

    /**
     * [DistanceFormatterOptions] to be used by [MapboxManeuverView].
     */
    var distanceFormatterOptions: DistanceFormatterOptions =
        DistanceFormatterOptions.Builder(context).build()

    /**
     * Options used to create [MapboxManeuverView] instance.
     */
    var options: ManeuverViewOptions = ManeuverViewOptions.Builder()
        .maneuverBackgroundColor(R.color.colorPrimary)
        .subManeuverBackgroundColor(R.color.colorPrimaryVariant)
        .turnIconManeuver(R.style.MapboxStyleTurnIconManeuver)
        .laneGuidanceTurnIconManeuver(R.style.MapboxStyleTurnIconManeuver)
        .stepDistanceTextAppearance(R.style.MapboxStyleStepDistance)
        .upcomingManeuverBackgroundColor(R.color.colorPrimary)
        .primaryManeuverOptions(
            ManeuverPrimaryOptions
                .Builder()
                .textAppearance(R.style.MapboxStylePrimaryManeuver)
                .exitOptions(
                    ManeuverExitOptions
                        .Builder()
                        .textAppearance(R.style.MapboxStyleExitTextForPrimary)
                        .build(),
                )
                .build(),
        )
        .secondaryManeuverOptions(
            ManeuverSecondaryOptions
                .Builder()
                .textAppearance(R.style.MapboxStyleSecondaryManeuver)
                .exitOptions(
                    ManeuverExitOptions
                        .Builder()
                        .textAppearance(R.style.MapboxStyleExitTextForSecondary)
                        .build(),
                )
                .build(),
        )
        .subManeuverOptions(
            ManeuverSubOptions
                .Builder()
                .textAppearance(R.style.MapboxStyleSubManeuver)
                .exitOptions(
                    ManeuverExitOptions
                        .Builder()
                        .textAppearance(R.style.MapboxStyleExitTextForSub)
                        .build(),
                )
                .build(),
        )
        .build()
}
