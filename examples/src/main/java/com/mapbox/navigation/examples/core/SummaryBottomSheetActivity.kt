package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.summary.SummaryBottomSheet
import kotlinx.android.synthetic.main.activity_summary_bottom_sheet.*
import java.lang.ref.WeakReference

/**
 * This activity shows how to integrate the
 * Navigation UI SDK's [SummaryBottomSheet] and
 * a camera re-centering button with the Navigation SDK.
 */
class SummaryBottomSheetActivity : AppCompatActivity(), OnMapReadyCallback {

    private val routeOverviewPadding by lazy { buildRouteOverviewPadding() }

    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private lateinit var destination: LatLng
    private lateinit var summaryBehavior: BottomSheetBehavior<SummaryBottomSheet>
    private lateinit var routeOverviewButton: ImageButton
    private lateinit var cancelBtn: AppCompatImageButton
    private val mapboxReplayer = MapboxReplayer()
    private var directionRoute: DirectionsRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary_bottom_sheet)
        initViews()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
            .locationEngine(getLocationEngine())
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions).apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRouteProgressObserver(routeProgressObserver)
        }

        initListeners()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxReplayer.finish()
        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)

        // This is not the most efficient way to preserve the route on a device rotation.
        // This is here to demonstrate that this event needs to be handled in order to
        // redraw the route line after a rotation.
        directionRoute?.let {
            outState.putString(Utils.PRIMARY_ROUTE_BUNDLE_KEY, it.toJson())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        directionRoute = Utils.getRouteFromBundle(savedInstanceState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(
                mapView,
                mapboxMap,
                this,
                true
            )

            when (directionRoute) {
                null -> {
                    if (shouldSimulateRoute()) {
                        mapboxNavigation
                            ?.registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
                        mapboxReplayer.pushRealLocation(this, 0.0)
                        mapboxReplayer.play()
                    }
                    mapboxNavigation
                        ?.navigationOptions
                        ?.locationEngine
                        ?.getLastLocation(locationListenerCallback)
                    Snackbar
                        .make(mapView, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
                        .show()
                }
                else -> restoreNavigation()
            }
        }

        mapboxMap.addOnMapLongClickListener { latLng ->
            destination = latLng
            mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
                mapboxNavigation?.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                        .alternatives(true)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build(),
                    routesReqCallback
                )
            }
            true
        }
    }

    // for testing else a real location engine is used.
    private fun getLocationEngine(): LocationEngine {
        return if (shouldSimulateRoute()) {
            ReplayLocationEngine(mapboxReplayer)
        } else {
            LocationEngineProvider.getBestLocationEngine(this)
        }
    }

    private fun initViews() {
        startNavigation.visibility = VISIBLE
        startNavigation.isEnabled = false
        summaryBottomSheet.visibility = GONE
        summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet).apply {
            isHideable = false
        }
        recenterBtn.hide()
        routeOverviewButton = findViewById(R.id.routeOverviewBtn)
        cancelBtn = findViewById(R.id.cancelBtn)
    }

    private fun updateViews(tripSessionState: TripSessionState) {
        when (tripSessionState) {
            TripSessionState.STARTED -> {
                startNavigation.visibility = GONE
                summaryBottomSheet.visibility = VISIBLE
                recenterBtn.hide()
            }
            TripSessionState.STOPPED -> {
                startNavigation.visibility = VISIBLE
                startNavigation.isEnabled = false
                summaryBottomSheet.visibility = GONE
                recenterBtn.hide()
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            navigationMapboxMap?.addOnCameraTrackingChangedListener(cameraTrackingChangedListener)
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.startTripSession()
        }

        summaryBehavior.setBottomSheetCallback(bottomSheetCallback)

        routeOverviewButton.setOnClickListener {
            navigationMapboxMap?.showRouteOverview(routeOverviewPadding)
            recenterBtn.show()
        }

        recenterBtn.addOnClickListener {
            recenterBtn.hide()
            navigationMapboxMap?.resetPadding()
            navigationMapboxMap
                ?.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
        }

        cancelBtn.setOnClickListener {
            mapboxNavigation?.stopTripSession()
            updateCameraOnNavigationStateChange(false)
        }
    }

    private fun buildRouteOverviewPadding(): IntArray {
        val leftRightPadding =
            resources
                .getDimension(
                    com.mapbox.navigation.ui.R.dimen.mapbox_route_overview_left_right_padding
                )
                .toInt()
        val paddingBuffer =
            resources
                .getDimension(
                    com.mapbox.navigation.ui.R.dimen.mapbox_route_overview_buffer_padding
                )
                .toInt()
        val instructionHeight =
            (
                resources
                    .getDimension(
                        com.mapbox.navigation.ui.R.dimen.mapbox_instruction_content_height
                    ) +
                    paddingBuffer
                )
                .toInt()
        val summaryHeight =
            resources
                .getDimension(com.mapbox.navigation.ui.R.dimen.mapbox_summary_bottom_sheet_height)
                .toInt()
        return intArrayOf(leftRightPadding, instructionHeight, leftRightPadding, summaryHeight)
    }

    private fun isLocationTracking(cameraMode: Int): Boolean {
        return cameraMode == CameraMode.TRACKING ||
            cameraMode == CameraMode.TRACKING_COMPASS ||
            cameraMode == CameraMode.TRACKING_GPS ||
            cameraMode == CameraMode.TRACKING_GPS_NORTH
    }

    private fun updateCameraOnNavigationStateChange(
        navigationStarted: Boolean
    ) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                updateLocationLayerRenderMode(RenderMode.COMPASS)
            }
        }
    }

    // Callbacks and Observers
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                directionRoute = routes[0]
                navigationMapboxMap?.drawRoute(routes[0])
                startNavigation.visibility = VISIBLE
                startNavigation.isEnabled = true
            } else {
                startNavigation.isEnabled = false
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    updateViews(TripSessionState.STARTED)
                }
                TripSessionState.STOPPED -> {
                    updateViews(TripSessionState.STOPPED)
                    navigationMapboxMap?.hideRoute()
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            summaryBottomSheet.update(routeProgress)
        }
    }

    private val cameraTrackingChangedListener = object : OnCameraTrackingChangedListener {
        override fun onCameraTrackingChanged(currentMode: Int) {
            if (isLocationTracking(currentMode)) {
                summaryBehavior.isHideable = false
                summaryBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        override fun onCameraTrackingDismissed() {
            if (mapboxNavigation?.getTripSessionState() == TripSessionState.STARTED) {
                summaryBehavior.isHideable = true
                summaryBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (summaryBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                recenterBtn.show()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    }

    private val locationListenerCallback = MyLocationEngineCallback(this)

    private class MyLocationEngineCallback(activity: SummaryBottomSheetActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            activityRef.get()?.navigationMapboxMap?.updateLocation(result.lastLocation)
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private fun shouldSimulateRoute(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .getBoolean(this.getString(R.string.simulate_route_key), false)
    }

    @SuppressLint("MissingPermission")
    private fun restoreNavigation() {
        directionRoute?.let {
            mapboxNavigation?.setRoutes(listOf(it))
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation?.startTripSession()
        }
    }
}
