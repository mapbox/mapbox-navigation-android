package com.mapbox.services.android.navigation.testapp.example.ui

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.location.Location
import android.support.design.widget.BottomSheetBehavior
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.navigation.testapp.NavigationApplication
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.example.utils.formatArrivalTime
import com.mapbox.services.android.navigation.ui.v5.camera.DynamicCamera
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import kotlin.math.roundToInt

class ExamplePresenter(private val view: ExampleView, private val viewModel: ExampleViewModel) {

  companion object {
    const val DEFAULT_ZOOM = 12.0
    const val DEFAULT_BEARING = 0.0
    const val DEFAULT_TILT = 0.0
    const val TWO_SECONDS = 2000
    const val ONE_SECOND = 1000
  }

  private var state: PresenterState = PresenterState.SHOW_LOCATION

  fun onPermissionResult(granted: Boolean) {
    if (granted) {
      view.initialize()
    } else {
      view.showPermissionDialog()
    }
  }

  fun onAutocompleteClick() {
    view.selectAllAutocompleteText()
    view.updateLocationFabVisibility(INVISIBLE)
    view.updateSettingsFabVisibility(INVISIBLE)
    view.updateAutocompleteBottomSheetHideable(false)
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
      view.addMapProgressChangeListener(viewModel.retrieveNavigation())
      view.updateNavigationFabVisibility(INVISIBLE)
      view.updateCancelFabVisibility(VISIBLE)
      view.updateNavigationDataVisibility(VISIBLE)
      view.updateAutocompleteBottomSheetHideable(true)
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
    view.updateLocationFabVisibility(VISIBLE)
    view.updateSettingsFabVisibility(VISIBLE)
    view.updateCancelFabVisibility(INVISIBLE)
    view.updateNavigationDataVisibility(INVISIBLE)
    view.updateAutocompleteBottomSheetHideable(false)
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
    if (state == PresenterState.NAVIGATE) {
      viewModel.startNavigation()
    }
  }

  fun onProgressUpdate(progress: RouteProgress?) {
    progress?.let {
      view.updateArrivalTime(it.formatArrivalTime(NavigationApplication.instance))
      val distance = it.currentLegProgress().currentStepProgress().distanceRemaining()
      view.updateStepDistanceRemaining("${distance.roundToInt()} m")
    }
  }

  fun onMapLongClick(point: LatLng) {
    viewModel.reverseGeocode(point)
  }

  fun onMilestoneUpdate(milestone: Milestone?) {
    milestone?.let {
      if (milestone is BannerInstructionMilestone) {
        val type = milestone.bannerInstructions.primary()?.type()
        val modifier = milestone.bannerInstructions.primary()?.modifier()
        view.updateManeuverView(type, modifier)
      }
    }
  }

  fun onBackPressed(): Boolean {
    if (!viewModel.collapsedBottomSheet) {
      view.updateAutocompleteBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
      return false
    }
    return true
  }

  fun onDestroy() {
    viewModel.onDestroy()
  }

  fun subscribe(owner: LifecycleOwner) {
    viewModel.location.observe(owner, Observer { onLocationUpdate(it) })
    viewModel.routes.observe(owner, Observer { onRouteFound(it) })
    viewModel.progress.observe(owner, Observer { onProgressUpdate(it) })
    viewModel.milestone.observe(owner, Observer { onMilestoneUpdate(it) })
    viewModel.geocode.observe(owner, Observer {
      it?.features()?.first()?.let {
        onDestinationFound(it)
      }
    })
    viewModel.activateLocationEngine()
  }

  fun buildDynamicCameraFrom(mapboxMap: MapboxMap) {
    // TODO fix this leak
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
      // left top right bottom
      val padding = intArrayOf(left, top, right, bottom)
      view.updateMapCameraFor(bounds, padding, TWO_SECONDS)
    }
  }
}