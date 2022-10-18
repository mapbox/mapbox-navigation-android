package com.mapbox.navigation.qa_test_app.view.customnavview

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ConstrainMode
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.GlyphsRasterizationOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.TileStoreUsageMode
import com.mapbox.maps.applyDefaultParams
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

fun toggleTheme(themeMode: Int) {
    AppCompatDelegate.setDefaultNightMode(themeMode)
}

fun customMapViewFromCode(context: Context): MapView {
    // set map options
    val mapOptions = MapOptions.Builder().applyDefaultParams(context)
        .constrainMode(ConstrainMode.HEIGHT_ONLY)
        .glyphsRasterizationOptions(
            GlyphsRasterizationOptions.Builder()
                .rasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                .fontFamily("sans-serif")
                .build()
        )
        .build()

    // set token and cache size for this particular map view, these settings will overwrite
    // the default value.
    val resourceOptions = ResourceOptions.Builder().applyDefaultParams(context)
        .accessToken(Utils.getMapboxAccessToken(context))
        .tileStoreUsageMode(TileStoreUsageMode.DISABLED)
        .build()

    // set initial camera position
    val initialCameraOptions = CameraOptions.Builder()
        .center(Point.fromLngLat(-122.4194, 37.7749))
        .zoom(9.0)
        .bearing(120.0)
        .build()

    val mapInitOptions = MapInitOptions(
        context = context,
        resourceOptions = resourceOptions,
        mapOptions = mapOptions,
        cameraOptions = initialCameraOptions,
        textureView = true
    )

    // create view programmatically and add to root layout
    val customMapView = MapView(context, mapInitOptions)
    customMapView.getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE)
    return customMapView
}

val Number.dp get() = com.mapbox.android.gestures.Utils.dpToPx(toFloat()).toInt()

fun Configuration.isNightMode() =
    uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

internal fun customRouteLineOptions(context: Context) =
    MapboxRouteLineOptions.Builder(context)
        .withRouteLineResources(
            RouteLineResources.Builder()
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .routeLowCongestionColor(Color.YELLOW)
                        .routeCasingColor(Color.RED)
                        .build()
                )
                .build()
        )
        .withRouteLineBelowLayerId("road-label") // for Style.LIGHT and Style.DARK
        .withVanishingRouteLineEnabled(true)
        .displaySoftGradientForTraffic(true)
        .build()

internal fun customRouteArrowOptions(context: Context) =
    RouteArrowOptions.Builder(context)
        .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
        .withArrowColor(Color.RED)
        .build()

internal fun customManeuverOptions() = ManeuverViewOptions
    .Builder()
    .maneuverBackgroundColor(R.color.maneuver_main_background)
    .subManeuverBackgroundColor(R.color.maneuver_sub_background)
    .upcomingManeuverBackgroundColor(R.color.maneuver_sub_background)
    .turnIconManeuver(R.style.MyCustomTurnIconManeuver)
    .laneGuidanceTurnIconManeuver(R.style.MyCustomTurnIconManeuver)
    .stepDistanceTextAppearance(R.style.MyCustomStepDistance)
    .primaryManeuverOptions(
        ManeuverPrimaryOptions
            .Builder()
            .textAppearance(R.style.MyCustomPrimaryManeuver)
            .exitOptions(
                ManeuverExitOptions
                    .Builder()
                    .textAppearance(R.style.MyCustomExitTextForPrimary)
                    .build()
            )
            .build()
    )
    .secondaryManeuverOptions(
        ManeuverSecondaryOptions
            .Builder()
            .textAppearance(R.style.MyCustomSecondaryManeuver)
            .exitOptions(
                ManeuverExitOptions
                    .Builder()
                    .textAppearance(R.style.MyCustomExitTextForSecondary)
                    .build()
            )
            .build()
    )
    .subManeuverOptions(
        ManeuverSubOptions
            .Builder()
            .textAppearance(R.style.MyCustomSubManeuver)
            .exitOptions(
                ManeuverExitOptions
                    .Builder()
                    .textAppearance(R.style.MyCustomExitTextForSub)
                    .build()
            )
            .build()
    )
    .build()
