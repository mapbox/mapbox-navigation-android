package com.mapbox.navigation.dropin

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.resources.TextAppearance
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.arrival.ArrivalTextComponent
import com.mapbox.navigation.dropin.map.geocoding.POINameComponent
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maneuver.model.MapboxExitProperties
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.puck.LocationPuckOptions
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
     * Provide custom [POINameComponent] [TextAppearance].
     * Use [defaultPoiNameTextAppearance] to reset to default.
     */
    @StyleRes
    var poiNameTextAppearance: Int? = null

    /**
     * Provide [PointAnnotationOptions] for destination marker.
     * Use [defaultDestinationMarkerAnnotationOptions] to reset to default.
     */
    var destinationMarkerAnnotationOptions: PointAnnotationOptions? = null

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
     * Provide custom [MapboxExtendableButton] style for the compass button.
     * Use [defaultCompassButtonStyle] to reset to default.
     */
    @StyleRes
    var compassButtonStyle: Int? = null

    /**
     * Provide custom [MapboxExtendableButton] style for the re-center button.
     * Use [defaultRecenterButtonStyle] to reset to default.
     */
    @StyleRes
    var recenterButtonStyle: Int? = null

    /**
     * Provide custom [MapboxCameraModeButton] style for the camera mode button.
     * Use [defaultCameraModeButtonStyle] to reset to default.
     */
    @StyleRes
    var cameraModeButtonStyle: Int? = null

    /**
     * Provide custom [MapboxExtendableButton] style for the route preview button.
     * Use [defaultRoutePreviewButtonStyle] to reset to default.
     */
    @StyleRes
    var routePreviewButtonStyle: Int? = null

    /**
     * Provide custom [MapboxAudioGuidanceButton] style for the audio guidance button.
     * Use [defaultAudioGuidanceButtonStyle] to reset to default.
     */
    @StyleRes
    var audioGuidanceButtonStyle: Int? = null

    /**
     * Provide custom [MapboxExtendableButton] style for the end navigation button.
     * Use [defaultEndNavigationButtonStyle] to reset to default.
     */
    @StyleRes
    var endNavigationButtonStyle: Int? = null

    /**
     * Provide custom [MapboxExtendableButton] style for the start navigation button.
     * Use [defaultStartNavigationButtonStyle] to reset to default.
     */
    @StyleRes
    var startNavigationButtonStyle: Int? = null

    /**
     * Provide custom [ManeuverViewOptions] to style [MapboxManeuverView].
     * Use [defaultManeuverViewOptions] to reset to default.
     */
    var maneuverViewOptions: ManeuverViewOptions? = null

    /**
     * Provide custom [ArrivalTextComponent] [TextAppearance].
     * Use [defaultArrivalTextAppearance] to reset to default.
     */
    @StyleRes
    var arrivalTextAppearance: Int? = null

    /**
     * Provide [LocationPuckOptions] containing references to either [LocationPuck2D] or
     * [LocationPuck3D] for location puck in each of the different navigation states to be
     * displayed on top of the [MapView].
     * Use [defaultLocationPuckOptions] to reset to default.
     */
    var locationPuckOptions: LocationPuckOptions? = null

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
         * Default [PointAnnotationOptions] for showing destination marker.
         */
        fun defaultDestinationMarkerAnnotationOptions(context: Context): PointAnnotationOptions =
            PointAnnotationOptions().apply {
                withIconImage(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.mapbox_ic_destination_marker
                    )!!.toBitmap()
                )
                withIconAnchor(IconAnchor.BOTTOM)
            }

        /**
         * Default [MapboxRoadNameView] background.
         */
        @DrawableRes
        fun defaultRoadNameBackground(): Int = R.drawable.mapbox_road_name_view_background

        /**
         * Default [POINameComponent] [TextAppearance].
         */
        @StyleRes
        fun defaultPoiNameTextAppearance(): Int = R.style.DropInInfoPanelHeadlineTextAppearance

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
         * Default [MapboxExtendableButton] style for the compass button.
         */
        @StyleRes
        fun defaultCompassButtonStyle(): Int = R.style.DropInStyleCompassButton

        /**
         * Default [MapboxCameraModeButton] style for the camera mode button.
         */
        @StyleRes
        fun defaultCameraModeButtonStyle(): Int = R.style.DropInStyleCameraModeButton

        /**
         * Default [MapboxExtendableButton] style for the re-center button.
         */
        @StyleRes
        fun defaultRecenterButtonStyle(): Int = R.style.DropInStyleRecenterButton

        /**
         * Default [MapboxExtendableButton] style for the route preview button.
         */
        @StyleRes
        fun defaultRoutePreviewButtonStyle(): Int = R.style.DropInStylePreviewButton

        /**
         * Default [MapboxExtendableButton] style for the end navigation button.
         */
        @StyleRes
        fun defaultEndNavigationButtonStyle(): Int = R.style.DropInStyleExitButton

        /**
         * Default [MapboxExtendableButton] style for the start navigation button.
         */
        @StyleRes
        fun defaultStartNavigationButtonStyle(): Int = R.style.DropInStyleStartButton

        /**
         * Default [MapboxAudioGuidanceButton] style for the audio guidance button.
         */
        @StyleRes
        fun defaultAudioGuidanceButtonStyle(): Int = R.style.DropInStyleAudioGuidanceButton

        /**
         * Default [ArrivalTextComponent] [TextAppearance].
         */
        @StyleRes
        fun defaultArrivalTextAppearance(): Int = R.style.DropInInfoPanelHeadlineTextAppearance

        /**
         * Default [ManeuverViewOptions] to style [MapboxManeuverView]
         */
        fun defaultManeuverViewOptions(): ManeuverViewOptions = ManeuverViewOptions
            .Builder()
            .maneuverBackgroundColor(R.color.colorPrimary)
            .subManeuverBackgroundColor(R.color.colorPrimaryVariant)
            .upcomingManeuverBackgroundColor(R.color.colorPrimaryVariant)
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
                            .mutcdExitProperties(defaultMutcdProperties())
                            .viennaExitProperties(defaultVienndProperties())
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
                            .mutcdExitProperties(defaultMutcdProperties())
                            .viennaExitProperties(defaultVienndProperties())
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
                            .mutcdExitProperties(defaultMutcdProperties())
                            .viennaExitProperties(defaultVienndProperties())
                            .build()
                    )
                    .build()
            )
            .build()

        /**
         * Default [LocationPuck] for [MapView].
         */
        fun defaultLocationPuckOptions(context: Context): LocationPuckOptions =
            LocationPuckOptions.Builder(context).build()

        private fun defaultMutcdProperties() = MapboxExitProperties.PropertiesMutcd(
            exitBackground = R.drawable.mapbox_dropin_exit_board_background,
            fallbackDrawable = R.drawable.mapbox_dropin_ic_exit_arrow_right_mutcd,
            exitRightDrawable = R.drawable.mapbox_dropin_ic_exit_arrow_right_mutcd,
            exitLeftDrawable = R.drawable.mapbox_dropin_ic_exit_arrow_left_mutcd
        )

        private fun defaultVienndProperties() = MapboxExitProperties.PropertiesVienna(
            exitBackground = R.drawable.mapbox_dropin_exit_board_background,
            fallbackDrawable = R.drawable.mapbox_dropin_ic_exit_arrow_right_vienna,
            exitRightDrawable = R.drawable.mapbox_dropin_ic_exit_arrow_right_vienna,
            exitLeftDrawable = R.drawable.mapbox_dropin_ic_exit_arrow_left_vienna
        )
    }
}
