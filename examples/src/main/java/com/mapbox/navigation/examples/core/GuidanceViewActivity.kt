package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
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
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.route.ReplayRouteLocationEngine
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_guidance_view.*

/**
 * Warning: This activity is for code demonstration. The guidance view api
 * is currently only supported by staging access_tokens, ask Mapbox for one.
 */
class GuidanceViewActivity : AppCompatActivity(), OnMapReadyCallback {

    private val replayRouteLocationEngine = ReplayRouteLocationEngine()
    private val origin: Point = Point.fromLngLat(139.7772481, 35.6818019)
    private val destination: Point = Point.fromLngLat(139.7756523, 35.6789722)

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationMapboxMap: NavigationMapboxMap

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
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

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            instructionView.updateDistanceWith(routeProgress)
            instructionView.determineGuidanceView(routeProgress)
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            navigationMapboxMap.drawRoutes(routes)
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guidance_view)

        initViews()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val accessToken = Utils.getMapboxAccessToken(this)
        val options = MapboxNavigation.defaultNavigationOptions(this, accessToken)
        mapboxNavigation = MapboxNavigation(
            applicationContext,
            accessToken,
            navigationOptions = options,
            locationEngine = replayRouteLocationEngine
        ).also {
            it.registerLocationObserver(locationObserver)
            it.registerRouteProgressObserver(routeProgressObserver)
            it.registerRoutesObserver(routesObserver)
            it.registerTripSessionStateObserver(tripSessionStateObserver)
            it.registerBannerInstructionsObserver(bannerInstructionObserver)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions
                        .builder(this@GuidanceViewActivity, style)
                        .useDefaultLocationEngine(false)
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap).also {
                it.setCamera(DynamicCamera(mapboxMap))
                it.addProgressChangeListener(mapboxNavigation)
            }
            val cameraPosition = CameraPosition.Builder()
                .target(LatLng(destination.latitude(), destination.longitude()))
                .zoom(16.5)
                .build()
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

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

    private fun initDynamicCamera(route: DirectionsRoute) {
        navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.GPS)
        navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
        navigationMapboxMap.startCamera(route)
    }

    @SuppressLint("MissingPermission")
    private fun initViews() {
        startNavigation.setOnClickListener {
            mapboxNavigation.startTripSession()
            mapboxNavigation.getRoutes().let { routes ->
                if (routes.isNotEmpty()) {
                    initDynamicCamera(routes[0])
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
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
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionObserver)
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
    }
}
