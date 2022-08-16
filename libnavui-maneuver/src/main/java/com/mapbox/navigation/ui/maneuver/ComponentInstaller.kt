package com.mapbox.navigation.ui.maneuver

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.maneuver.internal.ManeuverComponent
import com.mapbox.navigation.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView

/**
 * Install components that render [MapboxManeuverView].
 *
 * @param maneuverView
 * @param userId
 * @param styleId
 * @param config options that can be used to configure [ManeuverViewOptions]
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.maneuver(
    maneuverView: MapboxManeuverView,
    config: ManeuverConfig.() -> Unit = {}
): Installation {
    val componentConfig = ManeuverConfig().apply(config)
    return component(
        ManeuverComponent(
            maneuverView = maneuverView,
            userId = componentConfig.userId,
            styleId = componentConfig.styleId,
            componentConfig.options
        )
    )
}

/**
 * Maneuver view component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ManeuverConfig internal constructor() {
    /**
     * [userId]
     */
    var userId: String? = null

    /**
     * [styleId] used by maps style.
     */
    var styleId: String? = null

    /**
     * Options used to create [MapboxManeuverView] instance.
     */
    var options = ManeuverViewOptions.Builder()
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
                        .build()
                )
                .build()
        )
        .secondaryManeuverOptions(
            ManeuverSecondaryOptions
                .Builder()
                .textAppearance(R.style.MapboxStyleSecondaryManeuver)
                .exitOptions(
                    ManeuverExitOptions
                        .Builder()
                        .textAppearance(R.style.MapboxStyleExitTextForSecondary)
                        .build()
                )
                .build()
        )
        .subManeuverOptions(
            ManeuverSubOptions
                .Builder()
                .textAppearance(R.style.MapboxStyleSubManeuver)
                .exitOptions(
                    ManeuverExitOptions
                        .Builder()
                        .textAppearance(R.style.MapboxStyleExitTextForSub)
                        .build()
                )
                .build()
        )
        .build()
}
