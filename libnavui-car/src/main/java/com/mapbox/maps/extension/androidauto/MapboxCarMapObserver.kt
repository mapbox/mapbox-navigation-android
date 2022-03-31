package com.mapbox.maps.extension.androidauto

import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental

/**
 * Many downstream services will not work until the surface has been created and the map has
 * loaded. This interface allows you to create custom Mapbox experiences for the car.
 */
@MapboxExperimental
interface MapboxCarMapObserver {

  /**
   * Called when a [MapboxCarMapSurface] has been loaded.
   * You can assume there will only be a single surface at a time.
   *
   * @see [MapboxCarMap.registerObserver]
   *
   * @param mapboxCarMapSurface loaded and ready to use car map surface
   */
  fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
    // No op by default
  }

  /**
   * Called when a [MapboxCarMapSurface] has been detached from this observer. Some examples that
   * can cause this to detach are:
   * - [MapboxCarMap] lifecycle is destroyed
   * - This observer has been unregistered with [MapboxCarMap.unregisterObserver]
   *
   * You can assume that there was a corresponding call to [onAttached] with the same
   * [MapboxCarMapObserver] instance.
   *
   * @param mapboxCarMapSurface loaded and ready car map surface
   */
  fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
    // No op by default
  }

  /**
   * Called when the car library updates the visible regions for the surface. For example, this
   * is triggered when the action buttons come in and out of visibility.
   * You can assume this will be called after [onAttached].
   *
   * @param visibleArea the visible area provided by the host
   * @param edgeInsets distance from each side of the screen that creates the [visibleArea]
   */
  fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
    // No op by default
  }
}