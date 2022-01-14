package com.mapbox.navigation.example.wear

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.wear.widget.BoxInsetLayout
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures
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
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val infoText = findViewById<TextView>(R.id.infoText)
        val boxedContainer = findViewById<ViewGroup>(R.id.boxedContainer)
        val boxInsets = findViewById<BoxInsetLayout>(R.id.boxInsets)

        val mapView = findViewById<MapView>(R.id.mapView)
        val navigationLocationProvider = NavigationLocationProvider()
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        val mapboxMap = mapView.getMapboxMap()

        val navigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()
        )

        val viewportDataSource = MapboxNavigationViewportDataSource(
            mapView.getMapboxMap()
        )
        // TODO: update viewportDataSource.followingPadding based on boxInsets
        val navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.camera,
            viewportDataSource
        )
//        mapView.camera.addCameraAnimationsLifecycleListener(
//            NavigationBasicGesturesHandler(navigationCamera)
//        )
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

        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .build()
        val routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        val routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        val routeArrowView = MapboxRouteArrowView(routeArrowOptions)
        val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()

        navigation.registerRoutesObserver { result ->
            if (result.routes.isNotEmpty()) {
                // generate route geometries asynchronously and render them
                CoroutineScope(Dispatchers.Main).launch {
                    val result = routeLineAPI.setRoutes(
                        listOf(RouteLine(result.routes.first(), null))
                    )
                    val style = mapboxMap.getStyle()
                    if (style != null) {
                        routeLineView.renderRouteDrawData(style, result)
                    }
                }

                // update the camera position to account for the new route
                viewportDataSource.onRouteChanged(result.routes.first())
                viewportDataSource.evaluate()
            } else {
                // remove the route line and route arrow from the map
                val style = mapboxMap.getStyle()
                if (style != null) {
                    routeLineAPI.clearRouteLine { value ->
                        routeLineView.renderClearRouteLineValue(
                            style,
                            value
                        )
                    }
                    routeArrowView.render(style, routeArrowAPI.clearArrows())
                }

                // remove the route reference to change camera position
                viewportDataSource.clearRouteData()
                viewportDataSource.evaluate()
            }
        }

        navigation.registerRouteProgressObserver { routeProgress ->
            // update the camera position to account for the progressed fragment of the route
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()

            // show arrow on the route line with the next maneuver
            val maneuverArrowResult = routeArrowAPI.addUpcomingManeuverArrow(routeProgress)
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
            }
        }

        // load map style
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            routeLineView.initializeLayers(style)
        }

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
