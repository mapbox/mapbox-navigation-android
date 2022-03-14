package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityRerouteBinding
import com.mapbox.navigation.qa_test_app.utils.UserNavigationRerouteController
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.toPoint

class RerouteActivity : AppCompatActivity() {

    private val replayRouteMapper = ReplayRouteMapper()
    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    private var rerouteControllerType = RerouteControllerType.Default
        set(value) {
            if (field == value) return
            field = value
            when (field) {
                RerouteControllerType.Default -> {
                    mapboxNavigation.setRerouteController()
                }
                RerouteControllerType.Custom -> {
                    mapboxNavigation.setRerouteController(UserNavigationRerouteController(this))
                }
                RerouteControllerType.Disabled -> {
                    mapboxNavigation.setRerouteController(null)
                }
            }
            Log.d(TAG, "RerouteControllerType: $field")
        }

    private val binding by lazy { LayoutActivityRerouteBinding.inflate(layoutInflater) }

    private val sharedPrefs: SharedPreferences by lazy {
        getSharedPreferences("qa_feedback_activity", MODE_PRIVATE)
    }

    @get:UiThread
    private val isSimulate: Boolean
        get() = sharedPrefs.getBoolean(KEY_SP_IS_SIMULATED, SIMULATE_DEFAULT_VALUE)

    private val mapboxNavigation: MapboxNavigation by lazy {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .apply {
                    if (isSimulate) {
                        locationEngine(ReplayLocationEngine(mapboxReplayer))
                    }
                }
                .build()
        )
    }

    private val routeObserver by lazy {
        RoutesObserver { routesResult ->
            runOnUiThread {
                binding.mapView.getMapboxMap().getStyle {
                    setRoutesToStyles(it, routesResult.navigationRoutes)
                }
            }
        }
    }

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        binding.mapView.getMapboxMap().getStyle()?.apply {
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private val mapCamera: CameraAnimationsPlugin by lazy {
        binding.mapView.camera
    }

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) = Unit

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
        }
    }

    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }

    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(routeArrowOptions)
    }

    private val routeArrowApi: MapboxRouteArrowApi by lazy { MapboxRouteArrowApi() }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .displayRestrictedRoadSections(true)
            .withVanishingRouteLineEnabled(true)
            .build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder().restrictedRoadColor(Color.MAGENTA).build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            binding.mapView.getMapboxMap().getStyle()?.apply {
                routeLineView.renderRouteLineUpdate(this, result)
            }
        }

        val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        binding.mapView.getMapboxMap().getStyle()?.apply {
            routeArrowView.renderManeuverUpdate(this, arrowUpdate)
        }
    }

    private val rerouteStateObserver = RerouteController.RerouteStateObserver { state ->
        when (state) {
            is RerouteState.Failed -> {
                Log.e(TAG, "RerouteState: ${state.message}")
            }
            RerouteState.FetchingRoute -> {
                Log.d(TAG, "RerouteState: $state")
            }
            RerouteState.Idle -> Log.d(TAG, "RerouteState: $state")
            RerouteState.Interrupted -> Log.d(TAG, "RerouteState: $state")
            is RerouteState.RouteFetched -> {
                Log.d(
                    TAG,
                    "RerouteState: $state, origin: ${state.routerOrigin}"
                )
            }
        }
    }

    private companion object {
        private const val TAG = "RerouteActivity"
        private const val KEY_SP_IS_SIMULATED = "KEY_SP_REROUTE_ACTIVITY_IS_SIMULATED"
        private const val SIMULATE_DEFAULT_VALUE = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initViews()
        initNavigation()
        initStyle()
        initListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation.onDestroy()
    }

    private fun initViews() {
        binding.simulateRoute.isChecked = isSimulate
        Toast.makeText(
            this,
            "Simulation enabled: $isSimulate",
            Toast.LENGTH_SHORT
        )
            .show()

        binding.rerouteControllerChooser.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    rerouteControllerType = when (parent?.getItemAtPosition(position)) {
                        getString(R.string.reroute_controller_default) ->
                            RerouteControllerType.Default
                        getString(R.string.reroute_controller_custom) ->
                            RerouteControllerType.Custom
                        getString(R.string.reroute_controller_disabled) ->
                            RerouteControllerType.Disabled
                        else -> return
                    }.also {
                        Toast.makeText(
                            this@RerouteActivity,
                            "selected reroute type ${it.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun initNavigation() {
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        if (isSimulate) {
            mapboxNavigation.setRoutes(listOf(getRoute()))
        }

        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.getRerouteController()
            ?.registerRerouteStateObserver(rerouteStateObserver)
        mapboxNavigation.registerRoutesObserver(routeObserver)
    }

    private fun initStyle() {
        binding.mapView.getMapboxMap()
            .loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE) { style ->
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                    object : LocationEngineCallback<LocationEngineResult> {
                        override fun onSuccess(result: LocationEngineResult?) {
                            ifNonNull(
                                result?.lastLocation,
                                result?.locations
                            ) { lastLocation, locations ->
                                navigationLocationProvider.changePosition(
                                    lastLocation, locations, null, null
                                )
                                if (isSimulate) {
                                    val route = getRoute()
                                    setRoutesToStyles(style, listOf(route.toNavigationRoute()))
                                    val routeOrigin = Utils.getRouteOriginPoint(route)
                                    val cameraOptions = CameraOptions.Builder()
                                        .center(routeOrigin).zoom(14.0).build()
                                    binding.mapView.getMapboxMap().setCamera(cameraOptions)
                                } else {
                                    updateCamera(lastLocation, locations)
                                }
                            }
                        }

                        override fun onFailure(exception: Exception) = Unit
                    })
            }
    }

    private fun setRoutesToStyles(style: Style, routes: List<NavigationRoute>) {
        routeLineApi.setNavigationRoutes(routes) {
            routeLineView.renderRouteDrawData(style, it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        binding.startNavigation.setOnClickListener {
            mapboxNavigation.startTripSession()
            binding.startNavigation.visibility = View.GONE
            binding.simulateRoute.visibility = View.GONE
            binding.forceReroute.visibility = View.VISIBLE
            if (isSimulate) {
                startSimulation(mapboxNavigation.getRoutes()[0])
            }
        }
        binding.forceReroute.setOnClickListener {
            mapboxNavigation.getRerouteController()?.reroute { routes, origin ->
                runOnUiThread {
                    if (mapboxNavigation.getRerouteController() is
                        UserNavigationRerouteController
                    ) {
                        mapboxNavigation.setNavigationRoutes(routes)
                    }
                    Toast
                        .makeText(this, "forceReroute, origin = $origin", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        binding.mapView.getMapboxMap().addOnMapLongClickListener { destination ->
            navigationLocationProvider.lastLocation?.toPoint()?.let { origin ->
                requestRoute(origin, destination)
            } ?: run {
                Toast.makeText(this, "Origin location is not found", Toast.LENGTH_LONG)
                    .show()
            }
            return@addOnMapLongClickListener false
        }
        binding.simulateRoute.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean(KEY_SP_IS_SIMULATED, isChecked).apply()
            startActivity(Intent(this, this::class.java))
            finish()
        }
    }

    private fun updateCamera(location: Location, keyPoints: List<Location>) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(location.toPoint())
                .zoom(15.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    private fun requestRoute(origin: Point, destination: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(listOf(origin, destination))
                .build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setNavigationRoutes(routes)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Toast.makeText(
                        this@RerouteActivity,
                        "Route request is failed",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                }
            }
        )
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsString = Utils.readRawFileText(this, R.raw.warsaw_route)
        return DirectionsRoute.fromJson(routeAsString)
    }

    private enum class RerouteControllerType {
        Default,
        Custom,
        Disabled,
    }
}
