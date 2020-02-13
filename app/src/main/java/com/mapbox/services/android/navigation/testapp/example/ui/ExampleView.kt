package com.mapbox.services.android.navigation.testapp.example.ui

import android.location.Location
import android.view.View
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.search.autocomplete.OnFeatureClickListener
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.route.OnRouteSelectionChangeListener

interface ExampleView : PermissionsListener, OnMapReadyCallback,
    OnFeatureClickListener, OnRouteSelectionChangeListener {

    fun initialize()

    fun showSoftKeyboard()

    fun hideSoftKeyboard()

    fun showPermissionDialog()

    fun updateMapCamera(cameraUpdate: CameraUpdate, duration: Int)

    fun updateMapCameraFor(bounds: LatLngBounds, padding: IntArray, duration: Int)

    fun updateMapLocation(location: Location?)

    fun updateRoutes(routes: List<DirectionsRoute>)

    fun updateDestinationMarker(destination: Point)

    fun updateAutocompleteBottomSheetState(state: Int)

    fun updateAutocompleteProximity(location: Location?)

    fun selectAllAutocompleteText()

    fun updateLocationFabVisibility(visibility: Int)

    fun updateNavigationFabVisibility(visibility: Int)

    fun updateCancelFabVisibility(visibility: Int)

    fun updateSettingsFabVisibility(visibility: Int)

    fun updateInstructionViewVisibility(visibility: Int)

    fun updateInstructionViewWith(progress: RouteProgress)

    fun addMapProgressChangeListener(navigation: MapboxNavigation)

    fun removeRoute()

    fun clearMarkers()

    fun makeToast(message: String)

    fun transition()

    fun showSettings()

    fun adjustMapPaddingForNavigation()

    fun resetMapPadding()

    fun showAttributionDialog(attributionView: View)

    fun showAlternativeRoutes(alternativesVisible: Boolean)

    fun updateLocationRenderMode(@RenderMode.Mode renderMode: Int)

    fun updateCameraTrackingMode(@NavigationCamera.TrackingMode trackingMode: Int)
}
