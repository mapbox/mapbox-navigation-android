package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.NavigationMapboxMapInstanceState
import kotlinx.android.synthetic.main.activity_basic_navigation_layout.*

/**
 * This activity shows how to set up a basic turn-by-turn
 * navigation experience with the Navigation SDK and
 * Navigation UI SDK.
 */
open class BasicNavigationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val MAP_INSTANCE_STATE_KEY = "navgation_mapbox_map_instance_state"
    }
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var mapInstanceState: NavigationMapboxMapInstanceState? = null
    private var mapboxNavigation: MapboxNavigation? = null

    private val mapStyles = listOf(
        Style.MAPBOX_STREETS,
        Style.OUTDOORS,
        Style.LIGHT,
        Style.DARK,
        Style.SATELLITE_STREETS
    )

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_navigation_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        mapboxNavigation = if (savedInstanceState == null) {
            createMapboxNavigation()
        } else {
            MapboxNavigationProvider.retrieve()
        }

        initListeners()
    }

    private fun createMapboxNavigation(): MapboxNavigation {
        val optionsBuilder = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
        if (shouldSimulateRoute()) {
            optionsBuilder.locationEngine(ReplayLocationEngine())
        }
        return MapboxNavigationProvider.create(optionsBuilder.build()).apply {
            if (shouldSimulateRoute()) {
                mapboxReplayer.pushRealLocation(this@BasicNavigationActivity, 0.0)
                registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this, true)
            navigationMapboxMap?.addProgressChangeListener(MapboxNavigationProvider.retrieve())
            mapInstanceState?.let { state ->
                navigationMapboxMap?.restoreFrom(state)
            }

            mapboxNavigation?.registerTripSessionStateObserver(tripSessionStateObserver)
            mapboxNavigation?.registerRoutesObserver(routesObserver)
            if (shouldSimulateRoute()) {
                mapboxNavigation?.mapboxReplayer?.play()
            }
            mapboxNavigation?.getRoutes()?.firstOrNull()?.let {
                navigationMapboxMap?.drawRoute(it)
                navigationMapboxMap?.startCamera(it)
            } ?: Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
                .show()
        }
        mapboxMap.addOnMapLongClickListener { latLng ->
            mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
                mapboxNavigation?.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                        .alternatives(true)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build()
                )
            }
            true
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            updateNavigationButton()
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
            } else {
                navigationMapboxMap?.removeRoute()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun initListeners() {
        startNavigation.setOnClickListener {
            mapboxNavigation?.getRoutes()?.firstOrNull()?.let {
                navigationMapboxMap?.startCamera(it)
            }
            mapboxNavigation?.startActiveGuidance()
        }

        fabToggleStyle.setOnClickListener {
            navigationMapboxMap?.retrieveMap()?.setStyle(mapStyles.shuffled().first())
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        mapboxNavigation?.apply {
            unregisterTripSessionStateObserver(tripSessionStateObserver)
            unregisterRoutesObserver(routesObserver)
        }
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        navigationMapboxMap?.saveStateWith(MAP_INSTANCE_STATE_KEY, outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mapInstanceState = savedInstanceState.getParcelable(MAP_INSTANCE_STATE_KEY)
        navigationMapboxMap?.restoreFrom(mapInstanceState)
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        @SuppressLint("MissingPermission")
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    navigationMapboxMap?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                    updateCameraOnNavigationStateChange(true)
                }
                TripSessionState.STOPPED -> {
                    navigationMapboxMap?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NORTH)
                    updateCameraOnNavigationStateChange(false)
                    mapboxNavigation?.startFreeDrive()
                }
            }
            updateNavigationButton()
        }
    }

    private fun updateNavigationButton() {
        mapboxNavigation?.apply {
            val tripSessionState = getTripSessionState()
            val hasRoute = getRoutes().isNotEmpty()
            val shouldShowButton = tripSessionState == TripSessionState.STOPPED && hasRoute
            startNavigation.visibility = if (shouldShowButton) View.VISIBLE else View.GONE
        }
    }

    // Used to determine if the ReplayLocationEngine should be used to simulate the routing.
    // This is used for testing purposes.
    private fun shouldSimulateRoute(): Boolean = PreferenceManager
            .getDefaultSharedPreferences(this.applicationContext)
            .getBoolean(this.getString(R.string.simulate_route_key), false)

    private fun updateCameraOnNavigationStateChange(navigationStarted: Boolean) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                updateLocationLayerRenderMode(RenderMode.COMPASS)
            }
        }
    }
}
