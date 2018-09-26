package com.mapbox.services.android.navigation.testapp.example.ui

import android.location.Location
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.search.autocomplete.OnFeatureClickListener
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation

interface ExampleView: PermissionsListener, OnMapReadyCallback, OnFeatureClickListener {

  fun initialize()

  fun showSoftKeyboard()

  fun hideSoftKeyboard()

  fun showPermissionDialog()

  fun updateMapCamera(cameraUpdate: CameraUpdate, duration: Int)

  fun updateMapCameraFor(bounds: LatLngBounds, padding: IntArray, duration: Int)

  fun updateMapLocation(location: Location?)

  fun updateRoute(route: DirectionsRoute)

  fun updateDestinationMarker(destination: Point)

  fun updateAutocompleteBottomSheetHideable(isHideable: Boolean)

  fun updateAutocompleteBottomSheetState(state: Int)

  fun updateAutocompleteProximity(location: Location?)

  fun selectAllAutocompleteText()

  fun updateLocationFabVisibility(visibility: Int)

  fun updateDirectionsFabVisibility(visibility: Int)

  fun updateNavigationFabVisibility(visibility: Int)

  fun updateCancelFabVisibility(visibility: Int)

  fun updateSettingsFabVisibility(visibility: Int)

  fun updateNavigationDataVisibility(visibility: Int)

  fun updateManeuverView(maneuverType: String?, maneuverModifier: String?)

  fun updateStepDistanceRemaining(distance: String)

  fun updateArrivalTime(time: String)

  fun addMapProgressChangeListener(navigation: MapboxNavigation)

  fun removeRoute()

  fun clearMarkers()

  fun makeToast(message: String)

  fun showSettings()

  fun adjustMapPaddingForNavigation()

  fun resetMapPadding()
}