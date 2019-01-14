package com.mapbox.services.android.navigation.testapp.example.ui

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.pm.PackageManager
import android.location.Location
import android.support.design.widget.BottomSheetBehavior
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
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

  private var state: PresenterState = PresenterState.SHOW_LOCATION

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
    view.updateLocationFabVisibility(INVISIBLE)
    view.updateSettingsFabVisibility(INVISIBLE)
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

  fun onDirectionsFabClick() {
    state = PresenterState.FIND_ROUTE
    viewModel.findRouteToDestination()
  }

  fun onNavigationFabClick() {
    if (viewModel.canNavigate()) {
      state = PresenterState.NAVIGATE
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
    state = PresenterState.SHOW_LOCATION
    viewModel.stopNavigation()
    view.removeRoute()
    view.clearMarkers()
    view.resetMapPadding()
    view.updateLocationRenderMode(RenderMode.NORMAL)
    view.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
    view.updateLocationFabVisibility(VISIBLE)
    view.updateSettingsFabVisibility(VISIBLE)
    view.updateCancelFabVisibility(INVISIBLE)
    view.updateInstructionViewVisibility(INVISIBLE)
    view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
  }

  fun onAutocompleteBottomSheetStateChange(state: Int) {
    when (state) {
      BottomSheetBehavior.STATE_COLLAPSED -> {
        viewModel.collapsedBottomSheet = true
        view.hideSoftKeyboard()
        if (this.state == PresenterState.SHOW_LOCATION) {
          view.updateLocationFabVisibility(VISIBLE)
          view.updateSettingsFabVisibility(VISIBLE)
        }
      }
      BottomSheetBehavior.STATE_EXPANDED -> {
        viewModel.collapsedBottomSheet = false
      }
    }
  }

  fun onDestinationFound(feature: CarmenFeature) {
    feature.center()?.let {
      if (state == PresenterState.ROUTE_FOUND) {
        view.removeRoute()
        viewModel.primaryRoute = null
        view.updateNavigationFabVisibility(INVISIBLE)
      }
      state = PresenterState.SELECTED_DESTINATION
      viewModel.destination.value = it
      view.clearMarkers()
      view.hideSoftKeyboard()
      view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
      view.updateDestinationMarker(it)
      view.updateMapCamera(buildCameraUpdateFrom(it), TWO_SECONDS)
      view.updateLocationFabVisibility(INVISIBLE)
      view.updateSettingsFabVisibility(INVISIBLE)
      view.updateDirectionsFabVisibility(VISIBLE)
    }
  }

  fun onLocationUpdate(location: Location?) {
    location?.let {
      if (state == PresenterState.SHOW_LOCATION) {
        view.updateMapCamera(buildCameraUpdateFrom(location), TWO_SECONDS)
      }
      view.updateAutocompleteProximity(location)
      view.updateMapLocation(location)
    }
  }

  fun onRouteFound(routes: List<DirectionsRoute>?) {
    routes?.let { directionsRoutes ->
      when (state) {
        PresenterState.FIND_ROUTE -> {
          state = PresenterState.ROUTE_FOUND
          view.transition()
          view.showAlternativeRoutes(true)
          view.updateRoutes(directionsRoutes)
          view.updateDirectionsFabVisibility(INVISIBLE)
          view.updateNavigationFabVisibility(VISIBLE)
          viewModel.destination.value?.let { destination ->
            moveCameraToInclude(destination)
          }
        }
        PresenterState.NAVIGATE -> {
          view.updateRoutes(directionsRoutes)
        }
        else -> {
          // TODO no impl
        }
      }
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
    viewModel.reverseGeocode(point)
    return true
  }

  fun onBackPressed(): Boolean {
    if (!viewModel.collapsedBottomSheet) {
      view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
      return false
    }
    return true
  }

  fun subscribe(owner: LifecycleOwner) {
    viewModel.location.observe(owner, Observer { onLocationUpdate(it) })
    viewModel.routes.observe(owner, Observer { onRouteFound(it) })
    viewModel.progress.observe(owner, Observer { onProgressUpdate(it) })
    viewModel.milestone.observe(owner, Observer { onMilestoneUpdate(it) })
    viewModel.geocode.observe(owner, Observer { onGeocodingResponse(it) })
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

  private fun onGeocodingResponse(it: GeocodingResponse?) {
    val features = it?.features()
    val isValidFeatureList = features?.isNotEmpty() ?: false
    if (isValidFeatureList) {
      features?.first()?.let { firstFeature ->
        onDestinationFound(firstFeature)
      }
    }
  }
}