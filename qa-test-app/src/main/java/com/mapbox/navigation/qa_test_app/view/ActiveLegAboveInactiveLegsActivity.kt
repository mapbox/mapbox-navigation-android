package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.ActiveLegAboveOthersActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

class ActiveLegAboveInactiveLegsActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "ActiveLegAboveInactiveLegsActivity"
    }

    private val binding: ActiveLegAboveOthersActivityLayoutBinding by lazy {
        ActiveLegAboveOthersActivityLayoutBinding.inflate(layoutInflater)
    }

    private val navigationLocationProvider = NavigationLocationProvider()
    private val replayRouteMapper = ReplayRouteMapper()
    private val mapboxReplayer = MapboxReplayer()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val mapCamera: CameraAnimationsPlugin by lazy {
        binding.mapView.camera
    }

    private val locationComponent by lazy {
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    private val routeLineColorResources by lazy {
        val traveledColor = if (binding.chkTransparentVanishingRouteLine.isChecked) {
            Color.TRANSPARENT
        } else {
            Color.LTGRAY
        }
        val traveledCasingColor = if (binding.chkTransparentVanishingRouteLine.isChecked) {
            Color.TRANSPARENT
        } else {
            Color.DKGRAY
        }

        RouteLineColorResources.Builder()
            .inActiveRouteLegsColor(Color.YELLOW)
            .inactiveRouteLegCasingColor(Color.RED)
            .routeLineTraveledColor(traveledColor)
            .routeLineTraveledCasingColor(traveledCasingColor)
            .build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .styleInactiveRouteLegsIndependently(binding.chkStyleLegsDifferently.isChecked)
            .withVanishingRouteLineEnabled(binding.chkEnableVanishingRouteLine.isChecked)
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val navigationRoute: NavigationRoute by lazy {
        val routeAsString = Utils.readRawFileText(this, R.raw.multileg_route_with_overlap)
        DirectionsRoute.fromJson(routeAsString).toNavigationRoute(RouterOrigin.Offboard)
    }

    private val mapboxNavigation by requireMapboxNavigation(
        onCreatedObserver = object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                locationComponent.removeOnIndicatorPositionChangedListener(
                    onPositionChangedListener
                )
                mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.stopTripSession()
            }
        }
    ) {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initStyle()
        initListeners()
        locationComponent.locationPuck = LocationPuck2D(
            null,
            ContextCompat.getDrawable(
                this@ActiveLegAboveInactiveLegsActivity,
                R.drawable.mapbox_navigation_puck_icon
            ),
            null,
            null
        )
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        val startingLocation = Location("ReplayRoute").also {
            val routeOrigin = Utils.getRouteOriginPoint(navigationRoute.directionsRoute)
            it.latitude = routeOrigin.latitude()
            it.longitude = routeOrigin.longitude()
        }
        navigationLocationProvider.changePosition(startingLocation)
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.5)
        mapboxReplayer.play()
        binding.startNavigation.setOnClickListener {
            mapboxNavigation.setRerouteController(null)
            mapboxNavigation.startTripSession()
            mapboxNavigation.setNavigationRoutes(listOf(navigationRoute))
            binding.startNavigation.visibility = View.GONE
            locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)
            startSimulation(navigationRoute.directionsRoute)
        }
        binding.btnGetRoute.setOnClickListener {
            binding.mapView.getMapboxMap().getStyle { style ->
                routeLineApi.setNavigationRoutes(listOf(navigationRoute)) {
                    routeLineView.renderRouteDrawData(style, it)
                }
            }
            binding.btnGetRoute.visibility = View.GONE
            binding.viewOptionsContainer.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxReplayer.finish()
    }

    private fun initStyle() {
        binding.mapView.getMapboxMap().loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE
        ) { style ->
            val routeOrigin = Utils.getRouteOriginPoint(navigationRoute.directionsRoute)
            val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(17.0).build()
            binding.mapView.getMapboxMap().setCamera(cameraOptions)
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            //
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            if (locationMatcherResult.enhancedLocation.provider == "ReplayRoute") {
                navigationLocationProvider.changePosition(
                    locationMatcherResult.enhancedLocation,
                    locationMatcherResult.keyPoints,
                )
                updateCamera(locationMatcherResult.enhancedLocation)
            }
        }
    }

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .zoom(17.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->

        // This is the most important part of this example. The route progress will be used to
        // determine the active leg and adjust the route line visibility accordingly.
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            binding.mapView.getMapboxMap().getStyle()?.apply {
                routeLineView.renderRouteLineUpdate(this, result)
            }
        }
    }

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        binding.mapView.getMapboxMap().getStyle()?.apply {
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
    }
}
