package com.mapbox.navigation.examples.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
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
import com.mapbox.navigation.base.route.internal.RouteUrl
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import kotlinx.android.synthetic.main.activity_simple_mapbox_navigation.*
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import timber.log.Timber

class SimpleMapboxNavigationKt : AppCompatActivity(), OnMapReadyCallback {

    private var mapboxMap: MapboxMap? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private lateinit var mapboxNavigation: MapboxNavigation
    private var locationComponent: LocationComponent? = null
    private var symbolManager: SymbolManager? = null

    private lateinit var localLocationEngine: LocationEngine

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_mapbox_navigation)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        localLocationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)
        mapboxNavigation = MapboxNavigation(applicationContext, Utils.getMapboxAccessToken(this))
        startLocationUpdates()
        startNavigation.setOnClickListener {
            stopLocationUpdates()
            mapboxNavigation.startTripSession()
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))

        mapboxMap.addOnMapLongClickListener { click ->
            locationComponent?.lastKnownLocation?.let { location ->
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .baseUrl(RouteUrl.BASE_DIRECTIONS_STAGING_URL)
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(location.toPoint(), null, click.toPoint())
                        .alternatives(true)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build(),
                    routesReqCallback
                )

                symbolManager?.deleteAll()
                symbolManager?.create(
                    SymbolOptions()
                        .withIconImage("marker")
                        .withGeometry(click.toPoint())
                )
            }
            false
        }

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(this@SimpleMapboxNavigationKt, style)
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

    private fun startLocationUpdates() {
        val request = LocationEngineRequest.Builder(1000L)
            .setFastestInterval(500L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        try {
            localLocationEngine.requestLocationUpdates(
                request,
                locationEngineCallback,
                Looper.getMainLooper()
            )
            localLocationEngine.getLastLocation(locationEngineCallback)
        } catch (exception: SecurityException) {
            Timber.e(exception)
        }
    }

    private fun stopLocationUpdates() {
        localLocationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private val locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                locationComponent?.forceLocationUpdate(it)
            }
        }

        override fun onFailure(exception: java.lang.Exception) {
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            Timber.e("route progress %s", routeProgress.toString())
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            navigationMapRoute?.addRoutes(routes)
            if (routes.isEmpty()) {
                Toast.makeText(this@SimpleMapboxNavigationKt, "Empty routes", Toast.LENGTH_SHORT)
                    .show()
            }
            Timber.e("route changed %s", routes.toString())
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>): List<DirectionsRoute> {
            Timber.e("route request success %s", routes.toString())
            return routes
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            symbolManager?.deleteAll()
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            symbolManager?.deleteAll()
            Timber.e("route request canceled")
        }
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
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
