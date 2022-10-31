package com.mapbox.androidauto.location

import android.content.Context
import androidx.core.content.ContextCompat
import com.mapbox.androidauto.R
import com.mapbox.maps.plugin.LocationPuck2D

object CarLocationPuck {

    fun navigationPuck2D(context: Context) = LocationPuck2D(
        bearingImage = ContextCompat.getDrawable(context, R.drawable.mapbox_navigation_puck_icon)
    )

    // Example 2d compass puck
//    fun puck2D(carContext: CarContext) = LocationPuck2D(
//        topImage = carContext.getDrawable(R.drawable.mapbox_user_icon),
//        bearingImage = carContext.getDrawable(R.drawable.mapbox_user_bearing_icon),
//        shadowImage = carContext.getDrawable(R.drawable.mapbox_user_stroke_icon),
//    )

    // TODO Add a 3d model as an example, to the `assets` directory
//    private const val MODEL_SCALE = 0.2f
//    val puck3D: LocationPuck = LocationPuck3D(
//        modelUri = "asset://race_car_model.gltf",
//        modelScale = listOf(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE)
//    )
}
