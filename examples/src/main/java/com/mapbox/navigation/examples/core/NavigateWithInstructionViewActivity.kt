package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.ui.route.NavigationMapRoute
import kotlinx.android.synthetic.main.activity_navigate_instructionview.*
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import kotlinx.coroutines.channels.Channel

class NavigateWithInstructionViewActivity : AppCompatActivity(), OnMapReadyCallback {

    private val origin: Point = Point.fromLngLat(139.7772481, 35.6818019)
    private val destination: Point = Point.fromLngLat(139.7756523, 35.6789722)

    private val restartSessionEventChannel = Channel<RestartTripSessionAction>(1)
    private var mapboxMap: MapboxMap? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var locationComponent: LocationComponent? = null
    private var symbolManager: SymbolManager? = null

    private lateinit var mapboxNavigation: MapboxNavigation

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigate_instructionview)

        initViews()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val options = MapboxNavigation.defaultNavigationOptions(this, Mapbox.getAccessToken())
        mapboxNavigation = MapboxNavigation(
            applicationContext,
            Utils.getMapboxAccessToken(this),
            navigationOptions = options
        )
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView.onStart()

        restartSessionEventChannel.poll()?.also {
            mapboxNavigation.startTripSession()
        }

        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionObserver)

        if (mapboxNavigation.getRoutes().isEmpty() && mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
            // use this to kill the service and hide the notification when going into the background in the Free Drive state,
            // but also ensure to restart Free Drive when coming back from background by using the channel
            mapboxNavigation.stopTripSession()
            restartSessionEventChannel.offer(RestartTripSessionAction)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
        restartSessionEventChannel.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(destination.latitude(), destination.longitude()))
                .zoom(16.5)
                .build()
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        mapboxNavigation.requestRoutes(
            RouteOptions.builder().applyDefaultParams()
                .baseUrl("https://api-valhalla-route-staging.tilestream.net")
                .accessToken(Mapbox.getAccessToken()!!)
                .coordinates(origin, null, destination)
                .alternatives(true)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .bannerInstructions(true)
                .build(),
            routesReqCallback
        )

        symbolManager?.deleteAll()
        symbolManager?.create(
            SymbolOptions()
                .withIconImage("marker")
                .withGeometry(destination)
        )

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions
                        .builder(this@NavigateWithInstructionViewActivity, style)
                        .useDefaultLocationEngine(false)
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            navigationMapRoute = NavigationMapRoute(mapView, mapboxMap)
            navigationMapRoute?.setOnRouteSelectionChangeListener { route ->
                mapboxNavigation.setRoutes(mapboxNavigation.getRoutes().toMutableList().apply {
                    remove(route)
                    add(0, route)
                })
            }

            symbolManager = SymbolManager(mapView, mapboxMap, style)
            style.addImage("marker", IconFactory.getInstance(this).defaultMarker().bitmap)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initViews() {
        startNavigation.setOnClickListener {
            mapboxNavigation.startTripSession()
        }
    }

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
            navigationMapRoute?.addRoutes(routes)
            if (routes.isEmpty()) {
                Toast.makeText(
                    this@NavigateWithInstructionViewActivity,
                    "Empty routes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>): List<DirectionsRoute> {
            return routes
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            symbolManager?.deleteAll()
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            symbolManager?.deleteAll()
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

    private object RestartTripSessionAction
}
