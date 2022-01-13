package com.mapbox.navigation.example.wear

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val infoText = findViewById<TextView>(R.id.infoText)

        val mapView = findViewById<MapView>(R.id.mapView)
        val navigationLocationProvider = NavigationLocationProvider()
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }

        val navigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()
        )

        val viewportDataSource = MapboxNavigationViewportDataSource(
            mapView.getMapboxMap()
        )
        val navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.camera,
            viewportDataSource
        )
        mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        navigationCamera.requestNavigationCameraToFollowing()

        navigation.registerLocationObserver(object : LocationObserver {
            override fun onNewRawLocation(rawLocation: Location) { }
            override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
                navigationLocationProvider.changePosition(
                    location = locationMatcherResult.enhancedLocation,
                    keyPoints = locationMatcherResult.keyPoints,
                )

                viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
                viewportDataSource.evaluate()
            }
        })

        fun setRouteAndStartNavigation(route: DirectionsRoute) {
            navigation.startTripSession(true)
            navigation.setRoutes(listOf(route))
            navigation.registerRouteProgressObserver { routeProgress ->
                infoText.text = "Remaining: ${routeProgress.distanceRemaining}\n" +
                    "Instruciton: ${routeProgress?.bannerInstructions?.primary()?.text()}"
            }
        }
        navigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions(DirectionsCriteria.PROFILE_WALKING)
                .coordinates("18.613039,54.410983;18.608832,54.420143;18.602196,54.415626")
                .build(),
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    setRouteAndStartNavigation(routes.first())
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    // no impl
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }
            }
        )
    }
}
