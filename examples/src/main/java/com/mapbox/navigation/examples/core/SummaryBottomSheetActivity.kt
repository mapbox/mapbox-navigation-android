package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.location.ReplayRouteLocationEngine
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.summary.SummaryBottomSheet
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_summary_bottom_sheet.*
import timber.log.Timber

class SummaryBottomSheetActivity : AppCompatActivity(), OnMapReadyCallback {

    private val replayRouteLocationEngine by lazy { ReplayRouteLocationEngine() }
    private val routeOverviewPadding by lazy { buildRouteOverviewPadding() }

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var locationEngine: LocationEngine
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var destination: LatLng
    private lateinit var summaryBehavior: BottomSheetBehavior<SummaryBottomSheet>
    private lateinit var routeOverviewButton: ImageButton
    private lateinit var cancelBtn: AppCompatImageButton

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate savedInstanceState=%s", savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary_bottom_sheet)
        initViews()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        initNavigation()
        initListeners()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        stopLocationUpdates()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)

        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        Timber.d("onMapReady")
        this.mapboxMap = mapboxMap
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))

        mapboxMap.addOnMapLongClickListener { latLng ->
            Timber.d("onMapLongClickListener position=%s", latLng)
            destination = latLng
            locationComponent?.lastKnownLocation?.let { originLocation ->
                mapboxNavigation.requestRoutes(
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

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(
                        this@SummaryBottomSheetActivity,
                        style
                    )
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap).apply {
                addOnCameraTrackingChangedListener(cameraTrackingChangedListener)
                addProgressChangeListener(mapboxNavigation)
                setCamera(DynamicCamera(mapboxMap))
            }
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

    private fun initNavigation() {
        val accessToken = Utils.getMapboxAccessToken(this)
        mapboxNavigation = MapboxNavigation(
            applicationContext,
            accessToken,
            MapboxNavigation.defaultNavigationOptions(this, accessToken),
            replayRouteLocationEngine
        )
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startNavigation.setOnClickListener {
            Timber.d("start navigation")
            if (mapboxNavigation.getRoutes().isNotEmpty()) {
                replayRouteLocationEngine.assign(mapboxNavigation.getRoutes()[0])

                navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.GPS)
                navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])

                mapboxNavigation.startTripSession()
            }
        }

        summaryBehavior.setBottomSheetCallback(bottomSheetCallback)

        routeOverviewButton.setOnClickListener {
            navigationMapboxMap.showRouteOverview(routeOverviewPadding)
            recenterBtn.show()
        }

        recenterBtn.addOnClickListener {
            recenterBtn.hide()
            navigationMapboxMap.resetPadding()
            navigationMapboxMap.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
        }

        cancelBtn.setOnClickListener {
            mapboxNavigation.stopTripSession()
        }
    }

    private fun startLocationUpdates() {
        val requestLocationUpdateRequest =
            LocationEngineRequest.Builder(1000L)
                .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                .build()

        locationEngine.requestLocationUpdates(
            requestLocationUpdateRequest,
            locationListenerCallback,
            mainLooper
        )
        locationEngine.getLastLocation(locationListenerCallback)
    }

    private fun stopLocationUpdates() {
        locationEngine.removeLocationUpdates(locationListenerCallback)
    }

    private fun buildRouteOverviewPadding(): IntArray {
        val leftRightPadding =
            resources.getDimension(com.mapbox.libnavigation.ui.R.dimen.route_overview_left_right_padding)
                .toInt()
        val paddingBuffer =
            resources.getDimension(com.mapbox.libnavigation.ui.R.dimen.route_overview_buffer_padding)
                .toInt()
        val instructionHeight =
            (resources.getDimension(com.mapbox.libnavigation.ui.R.dimen.instruction_layout_height) + paddingBuffer).toInt()
        val summaryHeight =
            resources.getDimension(com.mapbox.libnavigation.ui.R.dimen.summary_bottomsheet_height)
                .toInt()
        return intArrayOf(leftRightPadding, instructionHeight, leftRightPadding, summaryHeight)
    }

    private fun isLocationTracking(cameraMode: Int): Boolean {
        return cameraMode == CameraMode.TRACKING ||
            cameraMode == CameraMode.TRACKING_COMPASS ||
            cameraMode == CameraMode.TRACKING_GPS ||
            cameraMode == CameraMode.TRACKING_GPS_NORTH
    }

    // Callbacks and Observers
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                navigationMapboxMap.drawRoute(routes[0])
                startNavigation.visibility = VISIBLE
                startNavigation.isEnabled = true
            } else {
                startNavigation.isEnabled = false
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            Timber.d("raw location %s", rawLocation.toString())
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (keyPoints.isNotEmpty()) {
                locationComponent?.forceLocationUpdate(keyPoints, true)
            } else {
                locationComponent?.forceLocationUpdate(enhancedLocation)
            }
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    updateViews(TripSessionState.STARTED)
                    stopLocationUpdates()
                    mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                }
                TripSessionState.STOPPED -> {
                    if (mapboxNavigation.getRoutes().isNotEmpty()) {
                        navigationMapboxMap.removeRoute()
                    }
                    updateViews(TripSessionState.STOPPED)
                    startLocationUpdates()
                    mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
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
            if (mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
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
            result.locations.firstOrNull()?.let { location ->
                Timber.d("location engine callback -> onSuccess location:%s", location)
                activityRef.get()?.locationComponent?.forceLocationUpdate(location)
            }
        }

        override fun onFailure(exception: Exception) {
            Timber.e("location engine callback -> onFailure(%s)", exception.localizedMessage)
        }
    }
}
