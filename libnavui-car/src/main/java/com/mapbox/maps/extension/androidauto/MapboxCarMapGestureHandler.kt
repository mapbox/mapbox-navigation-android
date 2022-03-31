package com.mapbox.maps.extension.androidauto

import androidx.car.app.SurfaceCallback
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.ScreenCoordinate

/**
 * This interface captures gesture events from Android Auto's SurfaceCallback. If you would like
 * to customize observe the map gestures provided, you can set your own gestures
 * with [MapboxCarMap.setGestureHandler].
 */
@MapboxExperimental
interface MapboxCarMapGestureHandler {

  /**
   * Allows you to implement or observe the map scroll gesture handler. The surface is
   * [MapboxCarMapObserver.onAttached] before this can be triggered.
   *
   * @see [SurfaceCallback.onScroll] for instructions to enable.
   *
   * @param mapboxCarMapSurface loaded and ready car map surface
   * @param distanceX the distance in pixels along the X axis
   * @param distanceY the distance in pixels along the Y axis
   */
  fun onScroll(
    mapboxCarMapSurface: MapboxCarMapSurface,
    visibleCenter: ScreenCoordinate,
    distanceX: Float,
    distanceY: Float
  ) {
    // Optional override
  }

  /**
   * Allows you to implement or observe the map fling gesture handler. The surface is
   * [MapboxCarMapObserver.onAttached] before this can be triggered.
   *
   * @see [SurfaceCallback.onFling] for instructions to enable.
   *
   * @param mapboxCarMapSurface loaded and ready car map surface
   * @param velocityX the velocity of this fling measured in pixels per second along the x axis
   * @param velocityY the velocity of this fling measured in pixels per second along the y axis
   */
  fun onFling(
    mapboxCarMapSurface: MapboxCarMapSurface,
    velocityX: Float,
    velocityY: Float
  ) {
    // Optional override
  }

  /**
   * Allows you to implement or observe the map scale gesture handler. The surface is
   * [MapboxCarMapObserver.onAttached] before this can be triggered.
   *
   * @see [SurfaceCallback.onScale] for instructions to enable.
   *
   * @param mapboxCarMapSurface loaded and ready car map surface
   * @param focusX x coordinate of the focal point in pixels. A negative value indicates that the focal point is unavailable.
   * @param focusY y coordinate of the focal point in pixels. A negative value indicates that the focal point is unavailable.
   * @param scaleFactor the scaling factor from the previous state to the current state during the scale event. This value is defined as (current state) / (previous state)
   */
  fun onScale(
    mapboxCarMapSurface: MapboxCarMapSurface,
    focusX: Float,
    focusY: Float,
    scaleFactor: Float
  ) {
    // Optional override
  }
}