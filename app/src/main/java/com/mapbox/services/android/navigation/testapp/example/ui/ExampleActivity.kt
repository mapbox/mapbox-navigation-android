package com.mapbox.services.android.navigation.testapp.example.ui

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.transition.TransitionManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.AttributionDialogManager
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.navigation.testapp.NavigationSettingsActivity
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.example.ui.autocomplete.AutoCompleteBottomSheetCallback
import com.mapbox.services.android.navigation.testapp.example.ui.autocomplete.ExampleAutocompleteAdapter
import com.mapbox.services.android.navigation.testapp.example.ui.permissions.PermissionRequestDialog
import com.mapbox.services.android.navigation.testapp.example.utils.hideKeyboard
import com.mapbox.services.android.navigation.testapp.example.utils.showKeyboard
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import kotlinx.android.synthetic.main.activity_example.*

class ExampleActivity : AppCompatActivity(), ExampleView {

  private val permissionsManager = PermissionsManager(this)
  private lateinit var presenter: ExamplePresenter
  private var map: NavigationMapboxMap? = null

  companion object {
    const val ZERO_PADDING = 0
    const val BOTTOMSHEET_MULTIPLIER = 4
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_example)
    setupWith(savedInstanceState)
  }

  public override fun onStart() {
    super.onStart()
    mapView.onStart()
    map?.onStart()
  }

  public override fun onResume() {
    super.onResume()
    mapView.onResume()
  }

  override fun onLowMemory() {
    super.onLowMemory()
    mapView.onLowMemory()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    mapView.onSaveInstanceState(outState)
  }

  public override fun onPause() {
    super.onPause()
    mapView.onPause()
  }

  public override fun onStop() {
    super.onStop()
    mapView.onStop()
    map?.onStop()
  }

  override fun onDestroy() {
    super.onDestroy()
    mapView.onDestroy()
    presenter.onDestroy()
  }

  override fun onBackPressed() {
    val exitActivity = presenter.onBackPressed()
    if (exitActivity) {
      super.onBackPressed()
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                          grantResults: IntArray) {
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
    // No impl - will use PermissionListener as our cue to show dialog
  }

  override fun onMapReady(mapboxMap: MapboxMap) {
    map = NavigationMapboxMap(mapView, mapboxMap)
    map?.setOnRouteSelectionChangeListener(this)
    mapboxMap.addOnMapLongClickListener { presenter.onMapLongClick(it) }
    presenter.buildDynamicCameraFrom(mapboxMap)
  }

  override fun onFeatureClicked(feature: CarmenFeature) {
    presenter.onDestinationFound(feature)
  }

  override fun onNewPrimaryRouteSelected(directionsRoute: DirectionsRoute) {
    presenter.onNewRouteSelected(directionsRoute)
  }

  override fun onPermissionResult(granted: Boolean) {
    presenter.onPermissionResult(granted)
  }

  override fun initialize() {
    presenter.subscribe(this)
    mapView.getMapAsync(this)
  }

  override fun showSoftKeyboard() {
    showKeyboard()
  }

  override fun hideSoftKeyboard() {
    hideKeyboard()
  }

  override fun showPermissionDialog() {
    PermissionRequestDialog(this, permissionsManager).show()
  }

  override fun updateMapCamera(cameraUpdate: CameraUpdate, duration: Int) {
    map?.retrieveMap()?.animateCamera(cameraUpdate, duration)
  }

  override fun updateMapCameraFor(bounds: LatLngBounds, padding: IntArray, duration: Int) {
    map?.retrieveMap()?.let {
      val position = it.getCameraForLatLngBounds(bounds, padding)
      it.animateCamera(CameraUpdateFactory.newCameraPosition(position), duration)
    }
  }

  override fun updateMapLocation(location: Location?) {
    map?.updateLocation(location)
  }

  override fun updateRoutes(routes: List<DirectionsRoute>) {
    map?.drawRoutes(routes)
  }

  override fun updateDestinationMarker(destination: Point) {
    map?.addMarker(this, destination)
  }

  override fun updateAutocompleteBottomSheetState(state: Int) {
    val behavior = BottomSheetBehavior.from(autocompleteBottomSheet)
    behavior.state = state
  }

  override fun updateAutocompleteBottomSheetHideable(isHideable: Boolean) {
    val behavior = BottomSheetBehavior.from(autocompleteBottomSheet)
    behavior.isHideable = isHideable
  }

  override fun updateAutocompleteProximity(location: Location?) {
    autocompleteView.updateProximity(location)
  }

  override fun selectAllAutocompleteText() {
    if (autocompleteView.text.isNotEmpty()) {
      autocompleteView.selectAll()
    }
  }

  override fun updateLocationFabVisibility(visibility: Int) {
    locationFab.visibility = visibility
  }

  override fun updateDirectionsFabVisibility(visibility: Int) {
    directionsFab.visibility = visibility
  }

  override fun updateNavigationFabVisibility(visibility: Int) {
    navigationFab.visibility = visibility
  }

  override fun updateCancelFabVisibility(visibility: Int) {
    cancelFab.visibility = visibility
  }

  override fun updateSettingsFabVisibility(visibility: Int) {
    settingsFab.visibility = visibility
  }

  override fun updateNavigationDataVisibility(visibility: Int) {
    TransitionManager.beginDelayedTransition(mainLayout)
    navigationDataCardView.visibility = visibility
  }

  override fun updateManeuverView(maneuverType: String?, maneuverModifier: String?) {
    maneuverView.setManeuverTypeAndModifier(maneuverType, maneuverModifier)
  }

  override fun updateStepDistanceRemaining(distance: String) {
    if (stepDistanceRemainingTextView.text != distance) {
      stepDistanceRemainingTextView.text = distance
    }
  }

  override fun updateArrivalTime(time: String) {
    if (arrivalTimeTextView.text != time) {
      arrivalTimeTextView.text = time
    }
  }

  override fun addMapProgressChangeListener(navigation: MapboxNavigation) {
    map?.addProgressChangeListener(navigation)
  }

  override fun removeRoute() {
    map?.removeRoute()
  }

  override fun clearMarkers() {
    map?.clearMarkers()
  }

  override fun makeToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }

  override fun showSettings() {
    startActivity(Intent(this, NavigationSettingsActivity::class.java))
  }

  override fun adjustMapPaddingForNavigation() {
    val mapViewHeight = mapView.height
    val bottomSheetHeight = resources.getDimension(R.dimen.bottom_sheet_peek_height).toInt()
    val topPadding = mapViewHeight - bottomSheetHeight * BOTTOMSHEET_MULTIPLIER
    map?.retrieveMap()?.setPadding(ZERO_PADDING, topPadding, ZERO_PADDING, ZERO_PADDING)
  }

  override fun resetMapPadding() {
    map?.retrieveMap()?.setPadding(ZERO_PADDING, ZERO_PADDING, ZERO_PADDING, ZERO_PADDING)
  }

  override fun showAttributionDialog(attributionView: View) {
    map?.retrieveMap()?.let {
      AttributionDialogManager(attributionView.context, it).onClick(attributionView)
    }
  }

  private fun setupWith(savedInstanceState: Bundle?) {
    val viewModel = ViewModelProviders.of(this).get(ExampleViewModel::class.java)
    presenter = ExamplePresenter(this, viewModel)

    mapView.onCreate(savedInstanceState)

    val behavior = BottomSheetBehavior.from(autocompleteBottomSheet)
    behavior.isHideable = false
    behavior.peekHeight = resources.getDimension(R.dimen.bottom_sheet_peek_height).toInt()
    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    behavior.setBottomSheetCallback(AutoCompleteBottomSheetCallback(presenter))

    autocompleteView.setOnClickListener { presenter.onAutocompleteClick() }
    autocompleteView.setAdapter(ExampleAutocompleteAdapter(this))
    autocompleteView.setFeatureClickListener(this)

    settingsFab.setOnClickListener { presenter.onSettingsFabClick() }
    locationFab.setOnClickListener { presenter.onLocationFabClick() }
    directionsFab.setOnClickListener { presenter.onDirectionsFabClick() }
    navigationFab.setOnClickListener { presenter.onNavigationFabClick() }
    cancelFab.setOnClickListener { presenter.onCancelFabClick() }
    attribution.setOnClickListener { presenter.onAttributionsClick(it) }

    val granted = PermissionsManager.areLocationPermissionsGranted(this)
    presenter.onPermissionResult(granted)
  }
}
