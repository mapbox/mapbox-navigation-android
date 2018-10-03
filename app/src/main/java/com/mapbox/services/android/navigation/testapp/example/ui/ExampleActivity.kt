package com.mapbox.services.android.navigation.testapp.example.ui

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.AttributionDialogManager
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.navigation.testapp.NavigationSettingsActivity
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.activity.HistoryActivity
import com.mapbox.services.android.navigation.testapp.example.ui.autocomplete.AutoCompleteBottomSheetCallback
import com.mapbox.services.android.navigation.testapp.example.ui.autocomplete.ExampleAutocompleteAdapter
import com.mapbox.services.android.navigation.testapp.example.ui.permissions.PermissionRequestDialog
import com.mapbox.services.android.navigation.testapp.example.utils.hideKeyboard
import com.mapbox.services.android.navigation.testapp.example.utils.showKeyboard
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import kotlinx.android.synthetic.main.activity_example.*

private const val ZERO_PADDING = 0
private const val BOTTOMSHEET_MULTIPLIER = 4

class ExampleActivity : HistoryActivity(), ExampleView {

  private var map: NavigationMapboxMap? = null
  private val viewModel by lazy(mode = LazyThreadSafetyMode.NONE) {
    ViewModelProviders.of(this).get(ExampleViewModel::class.java)
  }
  private val presenter by lazy(mode = LazyThreadSafetyMode.NONE) {
    ExamplePresenter(this, viewModel)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_example)
    setupWith(savedInstanceState)
    addNavigationForHistory(viewModel.retrieveNavigation())
  }

  public override fun onStart() {
    super.onStart()
    mapView.onStart()
    map?.onStart()
  }

  public override fun onResume() {
    super.onResume()
    mapView.onResume()
    viewModel.refreshOfflineVersionFromPreferences()
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
  }

  override fun onBackPressed() {
    val exitActivity = presenter.onBackPressed()
    if (exitActivity) {
      super.onBackPressed()
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                          grantResults: IntArray) {
    presenter.onPermissionResult(requestCode, grantResults)
  }

  override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
    // No impl - will use PermissionListener as our cue to show dialog
  }

  override fun onMapReady(mapboxMap: MapboxMap) {
    map = NavigationMapboxMap(mapView, mapboxMap)
    map?.setOnRouteSelectionChangeListener(this)
    map?.updateLocationLayerRenderMode(RenderMode.NORMAL)
    mapboxMap.addOnMapLongClickListener { presenter.onMapLongClick(it) }
    presenter.buildDynamicCameraFrom(mapboxMap)
    resetMapPadding() // Ignore navigation padding default
  }

  override fun onFeatureClicked(feature: CarmenFeature) {
    presenter.onDestinationFound(feature)
  }

  override fun onNewPrimaryRouteSelected(directionsRoute: DirectionsRoute) {
    presenter.onNewRouteSelected(directionsRoute)
  }

  override fun onPermissionResult(granted: Boolean) {
    presenter.onPermissionsGranted(granted)
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
    PermissionRequestDialog(this).show()
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

  override fun updateInstructionViewVisibility(visibility: Int) {
    instructionView.visibility = visibility
  }

  override fun updateInstructionViewWith(progress: RouteProgress) {
    instructionView.updateDistanceWith(progress)
  }

  override fun updateInstructionViewWith(milestone: Milestone) {
    instructionView.updateBannerInstructionsWith(milestone)
  }

  override fun addMapProgressChangeListener(navigation: MapboxNavigation) {
    map?.addProgressChangeListener(navigation)
  }

  override fun removeRoute() {
    map?.updateRouteVisibility(false)
  }

  override fun clearMarkers() {
    map?.clearMarkers()
  }

  override fun makeToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }

  override fun transition() {
    TransitionManager.beginDelayedTransition(mainLayout)
  }

  override fun showSettings() {
    startActivity(Intent(this, NavigationSettingsActivity::class.java))
  }

  override fun adjustMapPaddingForNavigation() {
    val mapViewHeight = mapView.height
    val bottomSheetHeight = resources.getDimension(R.dimen.bottom_sheet_peek_height).toInt()
    val topPadding = mapViewHeight - bottomSheetHeight * BOTTOMSHEET_MULTIPLIER
    val customPadding = intArrayOf(ZERO_PADDING, topPadding, ZERO_PADDING, ZERO_PADDING)
    map?.adjustLocationIconWith(customPadding)
  }

  override fun resetMapPadding() {
    val zeroPadding = intArrayOf(ZERO_PADDING, ZERO_PADDING, ZERO_PADDING, ZERO_PADDING)
    map?.adjustLocationIconWith(zeroPadding)
  }

  override fun showAttributionDialog(attributionView: View) {
    map?.retrieveMap()?.let {
      AttributionDialogManager(attributionView.context, it).onClick(attributionView)
    }
  }

  override fun showAlternativeRoutes(alternativesVisible: Boolean) {
    map?.showAlternativeRoutes(alternativesVisible)
  }

  override fun updateLocationRenderMode(renderMode: Int) {
    map?.updateLocationLayerRenderMode(renderMode)
  }

  override fun updateCameraTrackingMode(trackingMode: Int) {
    map?.updateCameraTrackingMode(trackingMode)
  }

  private fun setupWith(savedInstanceState: Bundle?) {
    mapView.onCreate(savedInstanceState)

    instructionView.retrieveFeedbackButton().hide()
    instructionView.retrieveSoundButton().hide()

    val behavior = BottomSheetBehavior.from(autocompleteBottomSheet)
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

    val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    val permissionGranted = PackageManager.PERMISSION_GRANTED
    val allPermissionsGranted =
        ContextCompat.checkSelfPermission(this, storagePermission) == permissionGranted &&
            ContextCompat.checkSelfPermission(this, locationPermission) == permissionGranted
    presenter.onPermissionsGranted(allPermissionsGranted)
  }
}
