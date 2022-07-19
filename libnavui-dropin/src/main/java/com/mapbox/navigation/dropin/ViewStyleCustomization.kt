package com.mapbox.navigation.dropin

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import com.google.android.material.resources.TextAppearance
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton

/**
 * A class that allows you to style the default standalone components used by [NavigationView].
 * If not specified, [NavigationView] uses the default styles defined for each of the standalone
 * components.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ViewStyleCustomization {
    /**
     * Specify info panel peek height.
     * Use [defaultInfoPanelPeekHeight] to reset to default.
     */
    @Px
    var infoPanelPeekHeight: Int? = null

    /**
     * Specify info panel start margin.
     * Use [defaultInfoPanelMarginStart] to reset to default.
     */
    @Px
    var infoPanelMarginStart: Int? = null

    /**
     * Specify info panel end margin.
     * Use [defaultInfoPanelMarginEnd] to reset to default.
     */
    @Px
    var infoPanelMarginEnd: Int? = null

    /**
     * Specify info panel background drawable.
     * Use [defaultInfoPanelBackground] to reset to default.
     */
    @DrawableRes
    var infoPanelBackground: Int? = null

    /**
     * Provide custom destination marker icon.
     * Use [defaultDestinationMarker] to reset to default.
     */
    @DrawableRes
    var destinationMarker: Int? = null

    /**
     * Provide custom [MapboxRoadNameView] background.
     * Use [defaultRoadNameBackground] to reset to default.
     */
    @DrawableRes
    var roadNameBackground: Int? = null

    /**
     * Provide custom [MapboxTripProgressView] style.
     * Use [defaultTripProgressStyle] to reset to default.
     */
    @StyleRes
    var tripProgressStyle: Int? = null

    /**
     * Provide custom [MapboxSpeedLimitView] style.
     * Use [defaultSpeedLimitStyle] to reset to default.
     */
    @StyleRes
    var speedLimitStyle: Int? = null

    /**
     * Provide custom [MapboxSpeedLimitView] [TextAppearance].
     * Use [defaultSpeedLimitTextAppearance] to reset to default.
     */
    @StyleRes
    var speedLimitTextAppearance: Int? = null

    /**
     * Provide custom [MapboxRoadNameView] [TextAppearance].
     * Use [defaultRoadNameTextAppearance] to reset to default.
     */
    @StyleRes
    var roadNameTextAppearance: Int? = null

    /**
     * Provide custom [MapboxExtendableButton] style for re-center button.
     * Use [defaultRecenterButtonStyle] to reset to default.
     */
    @StyleRes
    var recenterButtonStyle: Int? = null

    /**
     * Provide custom [MapboxCameraModeButton] style.
     * Use [defaultCameraModeButtonStyle] to reset to default.
     */
    @StyleRes
    var cameraModeButtonStyle: Int? = null

    /**
     * Provide custom [MapboxExtendableButton] style for route preview button.
     * Use [defaultRoutePreviewButtonStyle] to reset to default.
     */
    @StyleRes
    var routePreviewButtonStyle: Int? = null

    /**
     * Provide custom [MapboxAudioGuidanceButton] style.
     * Use [defaultAudioGuidanceButtonStyle] to reset to default.
     */
    @StyleRes
    var audioGuidanceButtonStyle: Int? = null

    /**
     * Provide custom [MapboxExtendableButton] style for end navigation button.
     * Use [defaultEndNavigationButtonStyle] to reset to default.
     */
    @StyleRes
    var endNavigationButtonStyle: Int? = null

    /**
     * Provide custom [MapboxExtendableButton] style for start navigation button.
     * Use [defaultStartNavigationButtonStyle] to reset to default.
     */
    @StyleRes
    var startNavigationButtonStyle: Int? = null

    /**
     * Provide custom [ManeuverViewOptions] to style [MapboxManeuverView].
     * Use [defaultManeuverViewOptions] to reset to default.
     */
    var maneuverViewOptions: ManeuverViewOptions? = null

    companion object {
        /**
         * Default info panel peek height in pixels.
         */
        @Px
        fun defaultInfoPanelPeekHeight(context: Context): Int =
            context.resources.getDimensionPixelSize(R.dimen.mapbox_infoPanel_peekHeight)

        /**
         * Default info panel start margin value.
         */
        @Px
        fun defaultInfoPanelMarginStart(): Int = 0

        /**
         * Default info panel end margin value.
         */
        @Px
        fun defaultInfoPanelMarginEnd(): Int = 0

        /**
         * Default info panel background drawable.
         */
        @DrawableRes
        fun defaultInfoPanelBackground(): Int = R.drawable.mapbox_bg_info_panel

        /**
         * Default destination marker icon.
         */
        @DrawableRes
        fun defaultDestinationMarker(): Int = R.drawable.mapbox_ic_destination_marker

        /**
         * Default [MapboxRoadNameView] background.
         */
        @DrawableRes
        fun defaultRoadNameBackground(): Int = R.drawable.mapbox_road_name_view_background

        /**
         * Default [MapboxTripProgressView] style.
         */
        @StyleRes
        fun defaultTripProgressStyle(): Int = R.style.DropInStyleTripProgressView

        /**
         * Default [MapboxSpeedLimitView] style.
         */
        @StyleRes
        fun defaultSpeedLimitStyle(): Int = R.style.DropInStyleSpeedLimit

        /**
         * Default [MapboxSpeedLimitView] [TextAppearance].
         */
        @StyleRes
        fun defaultSpeedLimitTextAppearance(): Int = R.style.DropInSpeedLimitTextAppearance

        /**
         * Default [MapboxRoadNameView] [TextAppearance].
         */
        @StyleRes
        fun defaultRoadNameTextAppearance(): Int = R.style.DropInRoadNameViewTextAppearance

        /**
         * Default [MapboxCameraModeButton] style.
         */
        @StyleRes
        fun defaultCameraModeButtonStyle(): Int = R.style.MapboxStyleCameraModeButton

        /**
         * Default [MapboxExtendableButton] style for re-center button.
         */
        @StyleRes
        fun defaultRecenterButtonStyle(): Int = R.style.DropInStyleRecenterButton

        /**
         * Default [MapboxExtendableButton] style for route preview button.
         */
        @StyleRes
        fun defaultRoutePreviewButtonStyle(): Int = R.style.DropInStylePreviewButton

        /**
         * Default [MapboxExtendableButton] style for end navigation button.
         */
        @StyleRes
        fun defaultEndNavigationButtonStyle(): Int = R.style.DropInStyleExitButton

        /**
         * Default [MapboxExtendableButton] style for start navigation button.
         */
        @StyleRes
        fun defaultStartNavigationButtonStyle(): Int = R.style.DropInStyleStartButton

        /**
         * Default [MapboxAudioGuidanceButton] style.
         */
        @StyleRes
        fun defaultAudioGuidanceButtonStyle(): Int = R.style.MapboxStyleAudioGuidanceButton

        /**
         * Default [ManeuverViewOptions] to style [MapboxManeuverView]
         */
        fun defaultManeuverViewOptions(): ManeuverViewOptions = ManeuverViewOptions
            .Builder()
            .maneuverBackgroundColor(R.color.colorPrimary)
            .subManeuverBackgroundColor(R.color.colorPrimaryVariant)
            .turnIconManeuver(R.style.DropInStyleTurnIconManeuver)
            .laneGuidanceTurnIconManeuver(R.style.DropInStyleTurnIconManeuver)
            .stepDistanceTextAppearance(R.style.DropInStyleStepDistance)
            .primaryManeuverOptions(
                ManeuverPrimaryOptions
                    .Builder()
                    .textAppearance(R.style.DropInStylePrimaryManeuver)
                    .exitOptions(
                        ManeuverExitOptions
                            .Builder()
                            .textAppearance(R.style.DropInStyleExitPrimary)
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
                            .textAppearance(R.style.DropInStyleExitSecondary)
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
                            .textAppearance(R.style.DropInStyleExitSub)
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
