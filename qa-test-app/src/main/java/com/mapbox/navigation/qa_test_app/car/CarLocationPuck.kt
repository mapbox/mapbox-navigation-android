package com.mapbox.navigation.qa_test_app.car

import android.annotation.SuppressLint
import androidx.car.app.CarContext
import com.mapbox.maps.R
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D

/**
 * Provides car location puck definitions.
 */
internal object CarLocationPuck {
  /**
   * 3D location puck with the real world size.
   */
  val duckLocationPuckHighZoom = LocationPuck3D(
    modelUri = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Duck/glTF-Embedded/Duck.gltf",
    modelScaleExpression = literal(listOf(10, 10, 10)).toJson(),
    modelRotation = listOf(0f, 0f, -90f)
  )

  /**
   * 3D location puck with a constant size across zoom levels.
   */
  val duckLocationPuckLowZoom = LocationPuck3D(
    modelUri = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Duck/glTF-Embedded/Duck.gltf",
    modelScale = listOf(0.2f, 0.2f, 0.2f),
    modelRotation = listOf(0f, 0f, -90f)
  )

  /**
   * Classic 2D location puck with blue dot and arrow.
   */
  @SuppressLint("UseCompatLoadingForDrawables")
  fun classicLocationPuck2D(carContext: CarContext) = LocationPuck2D(
    topImage = carContext.getDrawable(R.drawable.mapbox_user_icon),
    bearingImage = carContext.getDrawable(R.drawable.mapbox_user_bearing_icon),
    shadowImage = carContext.getDrawable(R.drawable.mapbox_user_stroke_icon)
  )
}
