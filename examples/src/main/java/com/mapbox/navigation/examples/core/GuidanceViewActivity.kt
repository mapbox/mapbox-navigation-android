package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.route.ReplayRouteLocationEngine
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_guidance_view.*
import kotlinx.android.synthetic.main.activity_guidance_view.mapView
import kotlinx.android.synthetic.main.activity_guidance_view.startNavigation

/**
 * This activity shows how to display Mapbox's guidance view images,
 * which visualize certain maneuvers, such as exiting off of a
 * highway. Access to guidance view images is limited to certain
 * Mapbox access tokens. Please contact Mapbox at
 * https://www.mapbox.com/contact/sales/ if you'd like a valid
 * access token.
 */
class GuidanceViewActivity : AppCompatActivity(), OnMapReadyCallback {

    private val replayRouteLocationEngine = ReplayRouteLocationEngine()
    private val origin: Point = Point.fromLngLat(139.7772481, 35.6818019)
    private val destination: Point = Point.fromLngLat(139.7756523, 35.6789722)

    private lateinit var mapboxNavigation: MapboxNavigation
    private var navigationMapboxMap: NavigationMapboxMap? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guidance_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val options = MapboxNavigation.defaultNavigationOptions(
                this,
                Utils.getMapboxAccessToken(this)
        )

        mapboxNavigation = MapboxNavigation(
                applicationContext,
                options,
                locationEngine = replayRouteLocationEngine
        ).also {
            it.registerRouteProgressObserver(routeProgressObserver)
            it.registerBannerInstructionsObserver(bannerInstructionObserver)
        }

        initListeners()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(destination.latitude(), destination.longitude()))
                    .zoom(16.5)
                    .build()
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this, true).also {
                it.addProgressChangeListener(mapboxNavigation)
            }

            // Ideally we should use Mapbox.getAccessToken(), but to show GuidanceView we need a
            // specific access token for route request.
            mapboxNavigation.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                            .accessToken(Utils.getMapboxAccessToken(this))
                            .coordinates(origin, null, destination)
                            .alternatives(true)
                            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                            .bannerInstructions(true)
                            .build(),
                    routesReqCallback
            )
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            instructionView.updateDistanceWith(routeProgress)
            instructionView.determineGuidanceView(routeProgress)
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            navigationMapboxMap?.drawRoute(routes[0])
            replayRouteLocationEngine.assign(routes[0])
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
                    startNavigation.visibility = View.GONE
                    if (mapboxNavigation.getRoutes().isNotEmpty()) {
                        instructionView.visibility = View.VISIBLE
                        instructionView.retrieveSoundButton().show()
                        instructionView.retrieveFeedbackButton().show()
                    }
                }
                TripSessionState.STOPPED -> {
                    instructionView.visibility = View.GONE
                    startNavigation.visibility = View.VISIBLE
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private val bannerInstructionObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            instructionView.updateBannerInstructionsWith(bannerInstructions)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation.startTripSession()
            mapboxNavigation.getRoutes().let { routes ->
                if (routes.isNotEmpty()) {
                    navigationMapboxMap?.startCamera(routes[0])
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionObserver)
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
        mapView.onDestroy()
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
