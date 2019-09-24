package com.mapbox.services.android.navigation.testapp.example.ui

import android.content.pm.PackageManager
import android.location.Location
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.navigation.testapp.NavigationApplication
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.example.ui.permissions.NAVIGATION_PERMISSIONS_REQUEST
import com.mapbox.services.android.navigation.ui.v5.camera.DynamicCamera
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

private const val DEFAULT_ZOOM = 12.0
private const val DEFAULT_BEARING = 0.0
private const val DEFAULT_TILT = 0.0
private const val TWO_SECONDS = 2000
private const val ONE_SECOND = 1000

class ExamplePresenter(private val view: ExampleView, private val viewModel: ExampleViewModel) {

  private var presenterState: PresenterState = PresenterState.SHOW_LOCATION

  fun onPermissionsGranted(granted: Boolean) {
    if (granted) {
      view.initialize()
    } else {
      view.showPermissionDialog()
    }
  }

  fun onPermissionResult(requestCode: Int, grantResults: IntArray) {
    if (requestCode == NAVIGATION_PERMISSIONS_REQUEST) {
      val granted = grantResults.isNotEmpty()
          && grantResults[0] == PackageManager.PERMISSION_GRANTED
          && grantResults[1] == PackageManager.PERMISSION_GRANTED
      onPermissionsGranted(granted)
    }
  }

  fun onAutocompleteClick() {
    view.selectAllAutocompleteText()
    view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_EXPANDED)
  }

  fun onAttributionsClick(attributionView: View) {
    view.showAttributionDialog(attributionView)
  }

  fun onSettingsFabClick() {
    view.showSettings()
  }

  fun onLocationFabClick() {
    viewModel.location.value?.let {
      view.updateMapCamera(buildCameraUpdateFrom(it), ONE_SECOND)
    }
  }

  fun onNavigationFabClick() {
    if (viewModel.canNavigate()) {
      presenterState = PresenterState.NAVIGATE
      view.showAlternativeRoutes(false)
      view.addMapProgressChangeListener(viewModel.retrieveNavigation())
      view.updateNavigationFabVisibility(INVISIBLE)
      view.updateCancelFabVisibility(VISIBLE)
      view.updateInstructionViewVisibility(VISIBLE)
      view.updateLocationRenderMode(RenderMode.GPS)
      view.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
      view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_HIDDEN)
      view.adjustMapPaddingForNavigation()
      viewModel.startNavigation()
    }
  }

  fun onCancelFabClick() {
    clearView()
  }

  private fun clearView() {
    presenterState = PresenterState.SHOW_LOCATION
    viewModel.stopNavigation()
    view.removeRoute()
    view.clearMarkers()
    view.resetMapPadding()
    view.updateLocationRenderMode(RenderMode.NORMAL)
    view.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
    view.updateLocationFabVisibility(VISIBLE)
    view.updateNavigationFabVisibility(INVISIBLE)
    view.updateSettingsFabVisibility(VISIBLE)
    view.updateCancelFabVisibility(INVISIBLE)
    view.updateInstructionViewVisibility(INVISIBLE)
    view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
  }

  fun onAutocompleteBottomSheetStateChange(newState: Int) {
    when (newState) {
      BottomSheetBehavior.STATE_COLLAPSED -> {
        view.hideSoftKeyboard()
        presenterState = PresenterState.SHOW_LOCATION
      }
      BottomSheetBehavior.STATE_EXPANDED -> {
        presenterState = PresenterState.SEARCH
      }
    }
  }

  fun onDestinationFound(feature: CarmenFeature) {
    feature.center()?.let {
      viewModel.destination.value = it
      view.hideSoftKeyboard()
      view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
      viewModel.findRouteToDestination()
    }
  }

  fun onLocationUpdate(location: Location?) {
    location?.let {
      if (presenterState == PresenterState.SHOW_LOCATION) {
        view.updateMapCamera(buildCameraUpdateFrom(location), TWO_SECONDS)
      }
      view.updateAutocompleteProximity(location)
      view.updateMapLocation(location)
    }
  }

  fun onRouteFound(routes: List<DirectionsRoute>?) {
    routes?.let { directionsRoutes ->
      if (presenterState != PresenterState.NAVIGATE) {
        view.clearMarkers()
        view.transition()
        view.showAlternativeRoutes(true)
        view.updateLocationFabVisibility(INVISIBLE)
        view.updateSettingsFabVisibility(INVISIBLE)
        view.updateNavigationFabVisibility(VISIBLE)
        viewModel.destination.value?.let { destination ->
          view.updateDestinationMarker(destination)
          moveCameraToInclude(destination)
        }
        presenterState = PresenterState.SHOW_ROUTE
      }
      view.updateRoutes(directionsRoutes)
    }
  }

  fun onNewRouteSelected(directionsRoute: DirectionsRoute) {
    viewModel.updatePrimaryRoute(directionsRoute)
  }

  fun onProgressUpdate(progress: RouteProgress?) {
    progress?.let {
      view.updateInstructionViewWith(it)
    }
  }

  fun onMilestoneUpdate(milestone: Milestone?) {
    milestone?.let {
      view.updateInstructionViewWith(it)
    }
  }

  fun onMapLongClick(point: LatLng): Boolean {
    viewModel.destination.value = Point.fromLngLat(point.longitude, point.latitude)
    viewModel.findRouteToDestination()
    return true
  }

  fun onBackPressed(): Boolean {
    when (presenterState) {
      PresenterState.SEARCH -> {
        view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
        return false
      }
      PresenterState.SHOW_ROUTE -> {
        clearView()
        return false
      }

      PresenterState.NAVIGATE -> {
        clearView()
        return false
      }

      else -> return true
    }
  }

  fun subscribe(owner: LifecycleOwner) {
    viewModel.location.observe(owner, Observer { onLocationUpdate(it) })
    viewModel.routes.observe(owner, Observer { onRouteFound(it) })
    viewModel.progress.observe(owner, Observer { onProgressUpdate(it) })
    viewModel.milestone.observe(owner, Observer { onMilestoneUpdate(it) })
    viewModel.activateLocationEngine()
  }

  fun buildDynamicCameraFrom(mapboxMap: MapboxMap) {
    viewModel.retrieveNavigation().cameraEngine = DynamicCamera(mapboxMap)
  }

  private fun buildCameraUpdateFrom(location: Location): CameraUpdate {
    return CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
            .zoom(DEFAULT_ZOOM)
            .target(LatLng(location.latitude, location.longitude))
            .bearing(DEFAULT_BEARING)
            .tilt(DEFAULT_TILT)
            .build())
  }

  private fun buildCameraUpdateFrom(point: Point): CameraUpdate {
    return CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
            .zoom(DEFAULT_ZOOM)
            .target(LatLng(point.latitude(), point.longitude()))
            .bearing(DEFAULT_BEARING)
            .tilt(DEFAULT_TILT)
            .build())
  }

  private fun moveCameraToInclude(destination: Point) {
    viewModel.location.value?.let {
      val origin = LatLng(it)
      val bounds = LatLngBounds.Builder()
              .include(origin)
              .include(LatLng(destination.latitude(), destination.longitude()))
              .build()

      val resources = NavigationApplication.instance.resources
      val left = resources.getDimension(R.dimen.route_overview_padding_left).toInt()
      val top = resources.getDimension(R.dimen.route_overview_padding_top).toInt()
      val right = resources.getDimension(R.dimen.route_overview_padding_right).toInt()
      val bottom = resources.getDimension(R.dimen.route_overview_padding_bottom).toInt()
      val padding = intArrayOf(left, top, right, bottom)
      view.updateMapCameraFor(bounds, padding, TWO_SECONDS)
    }
  }
}