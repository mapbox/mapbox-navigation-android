package com.mapbox.navigation.dropin

import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.google.android.material.resources.TextAppearance
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.view.MapboxAudioGuidanceButton
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.dropin.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView

/**
 * A class that allows you to style the default standalone components used by [NavigationView].
 * If not specified, [NavigationView] uses the default styles defined for each of the standalone
 * components.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ViewStyleCustomization {

    /**
     * Provide custom destination marker icon.
     * Use [defaultDestinationMarker] to reset to default.
     */
    @DrawableRes var destinationMarker: Int? = null
    /**
     * Provide custom [MapboxRoadNameView] background.
     * Use [defaultRoadNameBackground] to reset to default.
     */
    @DrawableRes var roadNameBackground: Int? = null
    /**
     * Provide custom [MapboxTripProgressView] style.
     * Use [defaultTripProgressStyle] to reset to default.
     */
    @StyleRes var tripProgressStyle: Int? = null
    /**
     * Provide custom [MapboxSpeedLimitView] style.
     * Use [defaultSpeedLimitStyle] to reset to default.
     */
    @StyleRes var speedLimitStyle: Int? = null
    /**
     * Provide custom [MapboxSpeedLimitView] [TextAppearance].
     * Use [defaultSpeedLimitTextAppearance] to reset to default.
     */
    @StyleRes var speedLimitTextAppearance: Int? = null
    /**
     * Provide custom [MapboxRoadNameView] [TextAppearance].
     * Use [defaultRoadNameTextAppearance] to reset to default.
     */
    @StyleRes var roadNameTextAppearance: Int? = null
    /**
     * Provide custom [MapboxExtendableButton] style for re-center button.
     * Use [defaultRecenterButtonStyle] to reset to default.
     */
    @StyleRes var recenterButtonStyle: Int? = null
    /**
     * Provide custom [MapboxCameraModeButton] style.
     * Use [defaultCameraModeButtonStyle] to reset to default.
     */
    @StyleRes var cameraModeButtonStyle: Int? = null
    /**
     * Provide custom [MapboxExtendableButton] style for route preview button.
     * Use [defaultRoutePreviewButtonStyle] to reset to default.
     */
    @StyleRes var routePreviewButtonStyle: Int? = null
    /**
     * Provide custom [MapboxAudioGuidanceButton] style.
     * Use [defaultAudioGuidanceButtonStyle] to reset to default.
     */
    @StyleRes var audioGuidanceButtonStyle: Int? = null
    /**
     * Provide custom [MapboxExtendableButton] style for end navigation button.
     * Use [defaultEndNavigationButtonStyle] to reset to default.
     */
    @StyleRes var endNavigationButtonStyle: Int? = null
    /**
     * Provide custom [MapboxExtendableButton] style for start navigation button.
     * Use [defaultStartNavigationButtonStyle] to reset to default.
     */
    @StyleRes var startNavigationButtonStyle: Int? = null
    /**
     * Provide custom [ManeuverViewOptions] to style [MapboxManeuverView].
     * Use [defaultManeuverViewOptions] to reset to default.
     */
    var maneuverViewOptions: ManeuverViewOptions? = null

    companion object {
        /**
         * Default destination marker icon.
         */
        fun defaultDestinationMarker() = R.drawable.mapbox_ic_destination_marker

        /**
         * Default [MapboxRoadNameView] background.
         */
        fun defaultRoadNameBackground() = R.drawable.mapbox_road_name_view_background

        /**
         * Default [MapboxTripProgressView] style.
         */
        fun defaultTripProgressStyle() = R.style.DropInStyleTripProgressView

        /**
         * Default [MapboxSpeedLimitView] style.
         */
        fun defaultSpeedLimitStyle() = R.style.DropInStyleSpeedLimit

        /**
         * Default [MapboxSpeedLimitView] [TextAppearance].
         */
        fun defaultSpeedLimitTextAppearance() = R.style.DropInSpeedLimitTextAppearance

        /**
         * Default [MapboxRoadNameView] [TextAppearance].
         */
        fun defaultRoadNameTextAppearance() = R.style.DropInRoadNameViewTextAppearance

        /**
         * Default [MapboxCameraModeButton] style.
         */
        fun defaultCameraModeButtonStyle() = R.style.MapboxStyleCameraModeButton

        /**
         * Default [MapboxExtendableButton] style for re-center button.
         */
        fun defaultRecenterButtonStyle() = R.style.DropInStyleRecenterButton

        /**
         * Default [MapboxExtendableButton] style for route preview button.
         */
        fun defaultRoutePreviewButtonStyle() = R.style.DropInStylePreviewButton

        /**
         * Default [MapboxExtendableButton] style for end navigation button.
         */
        fun defaultEndNavigationButtonStyle() = R.style.DropInStyleExitButton

        /**
         * Default [MapboxExtendableButton] style for start navigation button.
         */
        fun defaultStartNavigationButtonStyle() = R.style.DropInStyleStartButton

        /**
         * Default [MapboxAudioGuidanceButton] style.
         */
        fun defaultAudioGuidanceButtonStyle() = R.style.MapboxStyleAudioGuidanceButton

        /**
         * Default [ManeuverViewOptions] to style [MapboxManeuverView]
         */
        fun defaultManeuverViewOptions() = ManeuverViewOptions
            .Builder()
            .primaryManeuverOptions(
                ManeuverPrimaryOptions
                    .Builder()
                    .textAppearance(R.style.DropInStylePrimaryManeuver)
                    .exitOptions(
                        ManeuverExitOptions
                            .Builder()
                            .textAppearance(R.style.DropInStyleExitTextForPrimary)
                            .build()
                    )
                    .build()
            )
            .secondaryManeuverOptions(
                ManeuverSecondaryOptions
                    .Builder()
                    .textAppearance(R.style.DropInStyleSecondaryManeuver)
                    .exitOptions(
                        ManeuverExitOptions
                            .Builder()
                            .textAppearance(R.style.DropInStyleExitTextForSecondary)
                            .build()
                    )
                    .build()
            )
            .subManeuverOptions(
                ManeuverSubOptions
                    .Builder()
                    .textAppearance(R.style.DropInStyleSubManeuver)
                    .exitOptions(
                        ManeuverExitOptions
                            .Builder()
                            .textAppearance(R.style.DropInStyleExitTextForSub)
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
