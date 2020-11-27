package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngineCallback
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
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent.UI
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.BuildConfig
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.NavigationConstants
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheetListener
import com.mapbox.navigation.ui.feedback.FeedbackItem
import com.mapbox.navigation.ui.internal.utils.BitmapEncodeOptions
import com.mapbox.navigation.ui.internal.utils.ViewUtils
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_feedback_button.feedbackButton
import kotlinx.android.synthetic.main.activity_feedback_button.mapView
import kotlinx.android.synthetic.main.activity_feedback_button.screenshotView
import kotlinx.android.synthetic.main.activity_feedback_button.startNavigation
import java.lang.ref.WeakReference

/**
 * This activity shows how to integrate the [com.mapbox.navigation.ui.FeedbackButton]'s
 * feedback report flow with the Navigation SDK. Road closures and
 * traffic incidents are just two of the several types of live navigation
 * feedback that can be reported.
 */
@SuppressLint("MissingPermission")
class FeedbackButtonActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    FeedbackBottomSheetListener,
    ArrivalObserver {

    private var TAG = "FeedbackButtonActivity"
    private var mapboxMap: MapboxMap? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private lateinit var destination: LatLng
    private val mapboxReplayer = MapboxReplayer()
    private var directionRoute: DirectionsRoute? = null

    private var feedbackItem: FeedbackItem? = null
    private var feedbackEncodedScreenShot: String? = null
    private val feedbackList = ArrayList<FeedbackItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_button)
        initViews()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions).apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerArrivalObserver(this@FeedbackButtonActivity)
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
        mapboxNavigation?.unregisterArrivalObserver(this)
        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        directionRoute = Utils.getRouteFromBundle(savedInstanceState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap.Builder(mapView, mapboxMap, this)
                .vanishRouteLineEnabled(true)
                .build()

            mapboxNavigation?.registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
            mapboxReplayer.pushRealLocation(this, 0.0)
            mapboxReplayer.play()
            mapboxNavigation?.navigationOptions?.locationEngine?.getLastLocation(
                locationListenerCallback
            )

            directionRoute?.let { route ->
                navigationMapboxMap?.drawRoute(route)
                mapboxNavigation?.setRoutes(listOf(route))
                startNavigation.visibility = VISIBLE
                startNavigation.isEnabled = true
            }

            it.addImage(
                FEEDBACK_ICON_IMAGE_ID,
                AppCompatResources.getDrawable(this, R.drawable.mapbox_ic_feedback)!!
            )
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

        if (directionRoute == null) {
            Snackbar.make(mapView, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
                .show()
        }
    }

    override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
        // Not needed in this example
    }

    override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
        navigationMapboxMap?.clearMarkersWithIconImageProperty(FEEDBACK_ICON_IMAGE_ID)
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
            mapboxMap?.locationComponent?.lastKnownLocation?.let {
                navigationMapboxMap?.addCustomMarker(
                    SymbolOptions().withGeometry(it.toPoint()).withIconImage(FEEDBACK_ICON_IMAGE_ID)
                )
            }

            feedbackItem = null
            feedbackEncodedScreenShot = null
            feedbackButton.hide()
            supportFragmentManager.let {
                mapboxMap?.snapshot(this::encodeSnapshot)
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
            this.feedbackItem = feedback
            sendFeedback()
        }
    }

    private fun sendFeedback() {
        val feedback = feedbackItem
        val screenShot = feedbackEncodedScreenShot
        if (feedback != null && !screenShot.isNullOrEmpty()) {
            mapboxNavigation?.postUserFeedback(
                feedback.feedbackType,
                feedback.description,
                UI,
                screenShot,
                feedback.feedbackSubType.toTypedArray(),
                AppMetadata.Builder(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME).build()
            )
            showFeedbackSentSnackBar(context = this, view = mapView)
        }
    }

    private fun encodeSnapshot(snapshot: Bitmap) {
        screenshotView.visibility = VISIBLE
        screenshotView.setImageBitmap(snapshot)
        mapView.visibility = View.INVISIBLE
        feedbackEncodedScreenShot = ViewUtils.encodeView(
            ViewUtils.captureView(mapView),
            BitmapEncodeOptions.Builder()
                .width(400).compressQuality(40).build()
        )
        screenshotView.visibility = View.INVISIBLE
        mapView.visibility = VISIBLE

        sendFeedback()
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

    companion object {
        private const val FEEDBACK_ICON_IMAGE_ID = "marker-feedback"
    }
}

fun showFeedbackSentSnackBar(
    context: Context,
    view: View,
    @StringRes message: Int = R.string.mapbox_feedback_reported,
    length: Int = LENGTH_SHORT,
    setAnchorView: Boolean = false
) {
    val snackBar = Snackbar.make(
        view,
        message,
        length
    )

    if (setAnchorView) {
        snackBar.anchorView = view
    }

    snackBar.view.setBackgroundColor(
        ContextCompat.getColor(
            context,
            com.mapbox.navigation.ui.R.color.mapbox_feedback_bottom_sheet_secondary
        )
    )
    snackBar.setTextColor(
        ContextCompat.getColor(
            context,
            com.mapbox.navigation.ui.R.color.mapbox_feedback_bottom_sheet_primary_text
        )
    )

    snackBar.show()
}
