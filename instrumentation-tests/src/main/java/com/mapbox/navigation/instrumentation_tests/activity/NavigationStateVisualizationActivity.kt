package com.mapbox.navigation.instrumentation_tests.activity

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.coroutine.styleLoadedEvents
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.databinding.ActivityNavigationStateVisualizationBinding
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.shield.model.RouteShieldCallback
import com.mapbox.navigation.tripdata.speedlimit.api.MapboxSpeedInfoApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NavigationStateVisualizationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationStateVisualizationBinding
    private lateinit var mapboxMap: MapboxMap
    private lateinit var routeLineAPI: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private var requestedToFollow: Boolean = false
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity,
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            20.0 * pixelDensity,
            20.0 * pixelDensity,
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity,
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity,
        )
    }
    private val distanceFormatterOptions: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(this).unitType(UnitType.METRIC).build()
    }
    private val speedInfoApi = MapboxSpeedInfoApi()

    private val maneuverApi: MapboxManeuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatterOptions))
    }
    private val roadShieldCallback =
        RouteShieldCallback { shields -> binding.maneuverView.renderManeuverWith(shields) }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = locationMatcherResult.enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()
            if (!requestedToFollow) {
                navigationCamera.requestNavigationCameraToFollowing()
                requestedToFollow = true
            }

            speedInfoApi.updatePostedAndCurrentSpeed(
                locationMatcherResult,
                distanceFormatterOptions,
            )?.let {
                binding.speed.render(it)
            }
        }
    }

    private val routeProgressObserver =
        RouteProgressObserver { routeProgress ->
            // update the camera position to account for the progressed fragment of the route
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()

            routeLineAPI.updateWithRouteProgress(routeProgress) { update ->
                mapboxMap.style?.let { style ->
                    routeLineView.renderRouteLineUpdate(style, update)
                }
            }

            val maneuvers = maneuverApi.getManeuvers(routeProgress)
            maneuvers.fold(
                { _ ->
                    // do nothing
                },
                { maneuverList ->
                    maneuverApi.getRoadShields(maneuverList, roadShieldCallback)
                    binding.maneuverView.renderManeuvers(maneuvers)
                },
            )
        }

    private val routesObserver = RoutesObserver { result ->
        if (result.navigationRoutes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            CoroutineScope(Dispatchers.Main).launch {
                val result = routeLineAPI.setNavigationRoutes(result.navigationRoutes)
                val style = mapboxMap.style
                if (style != null) {
                    routeLineView.renderRouteDrawData(style, result)
                } else {
                    mapboxMap.styleLoadedEvents.first()
                    routeLineView.renderRouteDrawData(mapboxMap.style!!, result)
                }
            }

            // update the camera position to account for the new route
            viewportDataSource.onRouteChanged(result.navigationRoutes.first())
            viewportDataSource.evaluate()
        } else {
            // remove the route line and route arrow from the map
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeLineAPI.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value,
                    )
                }
            }

            // remove the route reference to change camera position
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationStateVisualizationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()

        // initialize the location puck
        binding.mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ImageHolder.Companion.from(
                    R.drawable.mapbox_navigation_puck_icon,
                ),
            )
            setLocationProvider(navigationLocationProvider)
            puckBearingEnabled = true
            enabled = true
            addOnIndicatorPositionChangedListener { point ->
                updateRouteLine(point)
            }
        }

        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap(),
        )
        viewportDataSource.options.followingFrameOptions
            .maximizeViewableGeometryWhenPitchZero = false
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource,
        )
        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera),
        )

        // initialize route line
        val mapboxRouteLinaApiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()
        val mapboxRouteLineViewOptions = MapboxRouteLineViewOptions.Builder(this)
            .routeLineBelowLayerId("road-label")
            .build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLinaApiOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineViewOptions)

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }

        // load map style
        mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            routeLineView.initializeLayers(style)
        }
    }

    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineAPI.cancel()
        routeLineView.cancel()
    }

    private fun updateRouteLine(point: Point) {
        val update = routeLineAPI.updateTraveledRouteLine(point)
        mapboxMap.style?.let {
            routeLineView.renderRouteLineUpdate(it, update)
        }
    }
}

private val mapboxNavigation get() = MapboxNavigationProvider.retrieve()
