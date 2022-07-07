package com.mapbox.navigation.qa_test_app.view.customnavview

import android.content.Context
import android.content.res.Configuration
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
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.NavigationStyles

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
