package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.route.ReplayRouteLocationEngine
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent.UI
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheetListener
import com.mapbox.navigation.ui.feedback.FeedbackItem
import com.mapbox.navigation.ui.legacy.NavigationConstants
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.utils.ViewUtils
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_feedback_button.*

class FeedbackButtonActivity : AppCompatActivity(), OnMapReadyCallback,
    FeedbackBottomSheetListener {

    private val replayRouteLocationEngine by lazy { ReplayRouteLocationEngine() }

    private var mapboxMap: MapboxMap? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private lateinit var destination: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_button)
        initViews()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
            this,
            Utils.getMapboxAccessToken(this)
        )

        mapboxNavigation = MapboxNavigation(
            applicationContext,
            mapboxNavigationOptions,
            replayRouteLocationEngine
        ).apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
        }

        initListeners()
        Snackbar.make(navigationLayout, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
            .show()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        navigationMapboxMap?.onStart()
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
        navigationMapboxMap?.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, true)
            LocationEngineProvider.getBestLocationEngine(this)
                .getLastLocation(locationListenerCallback)
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

    private fun initViews() {
        startNavigation.visibility = VISIBLE
        startNavigation.isEnabled = false
        feedbackButton.show()
    }

    private fun updateViews(tripSessionState: TripSessionState) {
        when (tripSessionState) {
            TripSessionState.STARTED -> {
                startNavigation.visibility = GONE
                feedbackButton.show()
            }
            TripSessionState.STOPPED -> {
                startNavigation.visibility = VISIBLE
                startNavigation.isEnabled = false
                feedbackButton.hide()
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.startTripSession()
            feedbackButton.show()
        }

        feedbackButton.addOnClickListener {
            feedbackButton.hide()
            supportFragmentManager.let {
                FeedbackBottomSheet.newInstance(
                    this,
                    NavigationConstants.FEEDBACK_BOTTOM_SHEET_DURATION
                ).show(it, FeedbackBottomSheet.TAG)
            }
        }
    }

    // Callbacks and Observers
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
                (mapboxNavigation?.locationEngine as ReplayRouteLocationEngine).assign(routes[0])
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
                    navigationMapboxMap?.removeRoute()
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private val locationListenerCallback = MyLocationEngineCallback(this)

    private class MyLocationEngineCallback(activity: FeedbackButtonActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            activityRef.get()?.navigationMapboxMap?.updateLocation(result.lastLocation)
        }

        override fun onFailure(exception: Exception) {
        }
    }

    override fun onFeedbackDismissed() {
        feedbackButton.show()
    }

    override fun onFeedbackSelected(feedbackItem: FeedbackItem?) {
        feedbackItem?.let { feedback ->
            mapboxMap?.snapshot { snapshot ->
                MapboxNavigation.postUserFeedback(
                    feedback.feedbackType,
                    feedback.description,
                    UI,
                    encodeSnapshot(snapshot)
                )
                showFeedbackSent()
            }
        }
    }

    private fun encodeSnapshot(snapshot: Bitmap): String {
        screenshotView.visibility = VISIBLE
        screenshotView.setImageBitmap(snapshot)
        mapView.visibility = View.INVISIBLE
        val encodedSnapshot = ViewUtils.encodeView(ViewUtils.captureView(mapView))
        screenshotView.visibility = View.INVISIBLE
        mapView.visibility = VISIBLE
        return encodedSnapshot
    }

    private fun showFeedbackSent() {
        val snackbar = Snackbar.make(
            mapView,
            com.mapbox.libnavigation.ui.R.string.feedback_reported,
            LENGTH_SHORT
        )

        snackbar.view.setBackgroundColor(
            ContextCompat.getColor(
                this,
                com.mapbox.libnavigation.ui.R.color.mapbox_feedback_bottom_sheet_snackbar
            )
        )
        snackbar.setTextColor(
            ContextCompat.getColor(
                this,
                com.mapbox.libnavigation.ui.R.color.mapbox_feedback_bottom_sheet_primary_text
            )
        )

        snackbar.show()
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
}
