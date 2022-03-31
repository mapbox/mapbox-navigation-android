package com.mapbox.maps.extension.androidauto

import androidx.car.app.SurfaceCallback
import com.mapbox.common.Logger
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.plugin.animation.camera

/**
 * This class contains the default map gestures. It Handles the gestures received from
 * [SurfaceCallback] and applies them to the [MapboxMap] camera. If you would like to customize
 * the map gestures, use [MapboxCarMap.setGestureHandler].
 */
@MapboxExperimental
open class DefaultMapboxCarMapGestureHandler : MapboxCarMapGestureHandler {

  /**
   * @see [MapboxCarMapGestureHandler.onScroll]
   * @see [SurfaceCallback.onScroll] for instructions to enable.
   *
   * @param mapboxCarMapSurface loaded and ready car map surface
   * @param distanceX the distance in pixels along the X axis
   * @param distanceY the distance in pixels along the Y axis
   */
  override fun onScroll(
    mapboxCarMapSurface: MapboxCarMapSurface,
    visibleCenter: ScreenCoordinate,
    distanceX: Float,
    distanceY: Float
  ) {
    with(mapboxCarMapSurface.mapSurface.getMapboxMap()) {
      dragStart(visibleCenter)
      val toCoordinate = ScreenCoordinate(
        visibleCenter.x - distanceX,
        visibleCenter.y - distanceY
      )
      Logger.i(TAG, "scroll from $visibleCenter to $toCoordinate")
      setCamera(getDragCameraOptions(visibleCenter, toCoordinate))
      dragEnd()
    }
  }

  /**
   * @see [MapboxCarMapGestureHandler.onFling]
   * @see [SurfaceCallback.onFling]
   *
   * @param mapboxCarMapSurface loaded and ready car map surface
   * @param velocityX the velocity of this fling measured in pixels per second along the x axis
   * @param velocityY the velocity of this fling measured in pixels per second along the y axis
   */
  override fun onFling(
    mapboxCarMapSurface: MapboxCarMapSurface,
    velocityX: Float,
    velocityY: Float
  ) {
    Logger.i(TAG, "fling $velocityX, $velocityY")
    // TODO implement fling
    // https://github.com/mapbox/mapbox-navigation-android-examples/issues/67
  }

  /**
   * @see [MapboxCarMapGestureHandler.onScale]
   * @see [SurfaceCallback.onScale]
   *
   * @param mapboxCarMapSurface loaded and ready car map surface
   * @param focusX x coordinate of the focal point in pixels. A negative value indicates that the focal point is unavailable.
   * @param focusY y coordinate of the focal point in pixels. A negative value indicates that the focal point is unavailable.
   * @param scaleFactor the scaling factor from the previous state to the current state during the scale event. This value is defined as (current state) / (previous state)
   */
  @Suppress("LongParameterList")
  override fun onScale(
    mapboxCarMapSurface: MapboxCarMapSurface,
    focusX: Float,
    focusY: Float,
    scaleFactor: Float
  ) {
    with(mapboxCarMapSurface.mapSurface) {
      val fromZoom = getMapboxMap().cameraState.zoom
      val toZoom = fromZoom - (1.0 - scaleFactor.toDouble())
      val anchor = ScreenCoordinate(
        focusX.toDouble(),
        focusY.toDouble()
      )

      val cameraOptions = CameraOptions.Builder()
        .zoom(toZoom)
        .anchor(anchor)
        .build()

      Logger.i(TAG, "scale with $focusX, $focusY $scaleFactor -> $fromZoom $toZoom")
      if (scaleFactor == DOUBLE_TAP_SCALE_FACTOR) {
        camera.easeTo(cameraOptions)
      } else {
        getMapboxMap().setCamera(cameraOptions)
      }
    }
  }

  private companion object {
    private const val TAG = "DefaultMapboxCarMapGestureHandler"

    /**
     * This appears to be undocumented from android auto. But when running from the emulator,
     * you can double tap the screen and zoom in to reproduce this value.
     * It is a jarring experience if you do not easeTo the zoom.
     */
    private const val DOUBLE_TAP_SCALE_FACTOR = 2.0f
  }
}