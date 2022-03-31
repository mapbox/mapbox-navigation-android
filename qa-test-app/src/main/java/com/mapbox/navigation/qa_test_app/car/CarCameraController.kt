package com.mapbox.navigation.qa_test_app.car

import android.graphics.Rect
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.androidauto.DefaultMapboxCarMapGestureHandler
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

/**
 * Controller class to handle map camera changes.
 */
@OptIn(MapboxExperimental::class)
class CarCameraController : MapboxCarMapObserver {

  private var lastGpsLocation: Point = HELSINKI
  private var previousCameraState: CameraState? = null
  private var isTrackingPuck = true

  private var surface: MapboxCarMapSurface? = null
  private var insets: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0)

  private val changePositionListener = OnIndicatorPositionChangedListener { point ->
    lastGpsLocation = point
    if (isTrackingPuck) {
      surface?.mapSurface?.getMapboxMap()?.setCamera(
        cameraOptions {
          center(point)
          padding(insets)
        }
      )
    }
  }

  private val changeBearingListener = OnIndicatorBearingChangedListener { bearing ->
    if (isTrackingPuck) {
      surface?.mapSurface?.getMapboxMap()?.setCamera(
        cameraOptions {
          bearing(bearing)
        }
      )
    }
  }

  val gestureHandler = object : DefaultMapboxCarMapGestureHandler() {
    override fun onScroll(
      mapboxCarMapSurface: MapboxCarMapSurface,
      visibleCenter: ScreenCoordinate,
      distanceX: Float,
      distanceY: Float
    ) {
      isTrackingPuck = false
    }
  }

  /**
   * Initialise the car camera controller with a map surface.
   */
  override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
    super.onAttached(mapboxCarMapSurface)
    this.surface = mapboxCarMapSurface
    mapboxCarMapSurface.mapSurface.getMapboxMap().setCamera(
      cameraOptions {
        pitch(previousCameraState?.pitch ?: INITIAL_PITCH)
        zoom(previousCameraState?.zoom ?: INITIAL_ZOOM)
        center(lastGpsLocation)
      }
    )
    with(mapboxCarMapSurface.mapSurface.location) {
      // Show a 3D location puck
      locationPuck = CarLocationPuck.duckLocationPuckLowZoom
      enabled = true
      addOnIndicatorPositionChangedListener(changePositionListener)
      addOnIndicatorBearingChangedListener(changeBearingListener)
    }
  }

  override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
    previousCameraState = mapboxCarMapSurface.mapSurface.getMapboxMap().cameraState
    with(mapboxCarMapSurface.mapSurface.location) {
      removeOnIndicatorPositionChangedListener(changePositionListener)
      removeOnIndicatorBearingChangedListener(changeBearingListener)
    }
    super.onDetached(mapboxCarMapSurface)
  }

  override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
    insets = edgeInsets
  }

  /**
   * Make camera center to location puck and track the location puck's position.
   */
  fun focusOnLocationPuck() {
    surface?.mapSurface?.camera?.flyTo(
      cameraOptions {
        center(lastGpsLocation)
      }
    )
    isTrackingPuck = true
  }

  /**
   * Function dedicated to zoom in map action buttons.
   */
  fun zoomInAction() = scaleEaseBy(ZOOM_ACTION_DELTA)

  /**
   * Function dedicated to zoom in map action buttons.
   */
  fun zoomOutAction() = scaleEaseBy(-ZOOM_ACTION_DELTA)

  private fun scaleEaseBy(delta: Double) {
    val mapSurface = surface?.mapSurface
    val fromZoom = mapSurface?.getMapboxMap()?.cameraState?.zoom ?: return
    val toZoom = (fromZoom + delta).coerceIn(MIN_ZOOM_OUT, MAX_ZOOM_IN)
    mapSurface.camera.easeTo(cameraOptions { zoom(toZoom) })
  }

  companion object {
    /**
     * Default location for the demo.
     */
    private val HELSINKI = Point.fromLngLat(24.9384, 60.1699)

    /**
     * Default zoom for the demo.
     */
    private const val INITIAL_ZOOM = 16.0

    /**
     * Constant camera pitch for the demo.
     */
    private const val INITIAL_PITCH = 75.0

    /**
     * When zooming the camera by a delta, this will prevent the camera from zooming further.
     */
    private const val MIN_ZOOM_OUT = 6.0

    /**
     * When zooming the camera by a delta, this will prevent the camera from zooming further.
     */
    private const val MAX_ZOOM_IN = 20.0

    /**
     * Simple zoom delta to associate with the zoom action buttons.
     */
    private const val ZOOM_ACTION_DELTA = 0.5
  }
}
