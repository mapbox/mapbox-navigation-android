package com.mapbox.navigation.examples.activity

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RouteObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import kotlinx.android.synthetic.main.activity_trip_service.*
import timber.log.Timber

class SimpleMapboxNavigationKt : AppCompatActivity(), OnMapReadyCallback {

    private var mapboxMap: MapboxMap? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private lateinit var mapboxNavigation: MapboxNavigation
    private var locationComponent: LocationComponent? = null
    private var symbolManager: SymbolManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_mapbox_navigation)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        mapboxNavigation = MapboxNavigation(applicationContext, Utils.getMapboxAccessToken(this))
        mapboxNavigation.startTripSession()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))

        mapboxMap.addOnMapLongClickListener { click ->
            locationComponent?.lastKnownLocation?.let { location ->
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(location.toPoint(), null, click.toPoint())
                        .build()
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

        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(this@SimpleMapboxNavigationKt, it)
                        .useDefaultLocationEngine(false)
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            navigationMapRoute = NavigationMapRoute(mapView, mapboxMap)
            symbolManager = SymbolManager(mapView, mapboxMap, it)
            it.addImage("marker", IconFactory.getInstance(this).defaultMarker().bitmap)
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            locationComponent?.forceLocationUpdate(rawLocation)
            Timber.e("raw location %s", rawLocation.toString())
        }

        override fun onEnhancedLocationChanged(enhancedLocation: Location) {
            Timber.e("enhanced location %s", enhancedLocation.toString())
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            Timber.e("route progress %s", routeProgress.toString())
        }
    }

    private val routeObserver = object : RouteObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            navigationMapRoute?.addRoute(routes[0])
            Timber.e("route changed %s", routes.toString())
        }

        override fun onRoutesRequested() {
            Timber.e("route requested")
        }

        override fun onRoutesRequestFailure(throwable: Throwable) {
            symbolManager?.deleteAll()
            Timber.e("route request failure %s", throwable.toString())
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
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRouteObserver(routeObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRouteObserver(routeObserver)
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
