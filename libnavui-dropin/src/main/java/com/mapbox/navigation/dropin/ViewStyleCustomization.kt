package com.mapbox.navigation.dropin

import android.content.Context
import android.widget.LinearLayout
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
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.arrival.ArrivalTextComponent
import com.mapbox.navigation.dropin.map.geocoding.POINameComponent
import com.mapbox.navigation.dropin.map.scalebar.MapboxMapScalebarParams
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maneuver.model.MapboxExitProperties
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
     * Provide custom parameters for [MapboxExtendableButton] to customize button styles.
     * Use [defaultRecenterButtonParams] to reset to default.
     */
    var recenterButtonParams: MapboxExtendableButtonParams? = null

    /**
     * Provide custom parameters for [MapboxCameraModeButton] to customize button styles.
     * Use [defaultCameraModeButtonParams] to reset to default.
     */
    var cameraModeButtonParams: MapboxExtendableButtonParams? = null

    /**
     * Provide custom parameters for [MapboxExtendableButton] to customize button styles.
     * Use [defaultRoutePreviewButtonParams] to reset to default.
     */
    var routePreviewButtonParams: MapboxExtendableButtonParams? = null

    /**
     * Provide custom parameters for [MapboxAudioGuidanceButton] to customize button styles.
     * Use [defaultAudioGuidanceButtonParams] to reset to default.
     */
    var audioGuidanceButtonParams: MapboxExtendableButtonParams? = null

    /**
     * Provide custom parameters for [MapboxExtendableButton] to customize button styles.
     * Use [defaultEndNavigationButtonParams] to reset to default.
     */
    var endNavigationButtonParams: MapboxExtendableButtonParams? = null

    /**
     * Provide custom parameters for [MapboxExtendableButton] to customize button styles.
     * Use [defaultStartNavigationButtonParams] to reset to default.
     */
    var startNavigationButtonParams: MapboxExtendableButtonParams? = null

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
     * Provide custom [LocationPuck] for [MapView].
     * Use [defaultLocationPuck] to reset to default.
     */
    var locationPuck: LocationPuck? = null

    /**
     * Map scalebar params.
     * Use [defaultMapScalebarParams] to reset to default.
     * NOTE: When `enabled`, the `scalebar` will always be added to the top start corner
     * of the screen. Position of the `scalebar` using `NavigationView` cannot be changed at any
     * given time. However, if you change the position using `MapView`, the behavior is undefined
     * and you will be responsible to ensure the correct positioning based on other view overlays.
     */
    var mapScalebarParams: MapboxMapScalebarParams? = null

    /**
     * Map compass params.
     * Use [defaultCompassButtonParams] to reset to default.
     */
    var compassButtonParams: MapboxExtendableButtonParams? = null

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
         * Default [MapboxCameraModeButton] params.
         */
        fun defaultCameraModeButtonParams(context: Context) = MapboxExtendableButtonParams(
            R.style.MapboxStyleCameraModeButton,
            context.defaultLayoutParams().apply {
                topMargin = context.defaultSpacing()
                bottomMargin = context.defaultSpacing()
            },
        )

        /**
         * Default [MapboxExtendableButton] params.
         */
        fun defaultRecenterButtonParams(context: Context) = MapboxExtendableButtonParams(
            R.style.DropInStyleRecenterButton,
            context.defaultLayoutParams().apply {
                topMargin = context.defaultSpacing()
                bottomMargin = context.defaultSpacing()
            },
        )

        /**
         * Default [MapboxAudioGuidanceButton] params.
         */
        fun defaultAudioGuidanceButtonParams(context: Context) = MapboxExtendableButtonParams(
            R.style.MapboxStyleAudioGuidanceButton,
            context.defaultLayoutParams().apply {
                topMargin = context.defaultSpacing()
                bottomMargin = context.defaultSpacing()
            },
        )

        /**
         * Default [MapboxExtendableButton] params.
         */
        fun defaultRoutePreviewButtonParams(context: Context) = MapboxExtendableButtonParams(
            R.style.DropInStylePreviewButton,
            context.defaultLayoutParams(),
        )

        /**
         * Default [MapboxExtendableButton] params.
         */
        fun defaultEndNavigationButtonParams(context: Context) = MapboxExtendableButtonParams(
            R.style.DropInStyleExitButton,
            context.defaultLayoutParams().apply {
                marginEnd =
                    context.resources.getDimensionPixelSize(R.dimen.mapbox_infoPanel_paddingEnd)
            },
        )

        /**
         * Default [MapboxExtendableButton] params.
         */
        fun defaultStartNavigationButtonParams(context: Context) = MapboxExtendableButtonParams(
            R.style.DropInStyleStartButton,
            context.defaultLayoutParams(),
        )

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
        fun defaultLocationPuck(context: Context): LocationPuck = LocationPuck2D(
            bearingImage = ContextCompat.getDrawable(
                context,
                R.drawable.mapbox_navigation_puck_icon,
            )
        )

        /**
         * Default map scalebar parameters.
         */
        fun defaultMapScalebarParams(context: Context): MapboxMapScalebarParams =
            MapboxMapScalebarParams.Builder(context).build()

        /**
         * Default map compass [MapboxExtendableButtonParams] parameters.
         */
        fun defaultCompassButtonParams(context: Context): MapboxExtendableButtonParams =
            MapboxExtendableButtonParams(
                R.style.DropInStyleCompassButton,
                context.defaultLayoutParams().apply {
                    topMargin = context.defaultSpacing()
                    bottomMargin = context.defaultSpacing()
                },
                false
            )

        private fun Context.defaultSpacing() =
            resources.getDimensionPixelSize(R.dimen.mapbox_actionList_spacing)

        private fun Context.defaultLayoutParams() = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.mapbox_extendable_button_width),
            resources.getDimensionPixelSize(R.dimen.mapbox_extendable_button_height)
        )

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
