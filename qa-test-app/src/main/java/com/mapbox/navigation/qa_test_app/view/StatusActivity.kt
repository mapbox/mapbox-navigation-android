package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityStatusBinding
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.NavigationStyles.NAVIGATION_DAY_STYLE
import com.mapbox.navigation.ui.maps.NavigationStyles.NAVIGATION_DAY_STYLE_ID
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.shield.model.RouteShieldCallback
import com.mapbox.navigation.ui.status.model.Status
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.LoggerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This activity demonstrates the usage of the [MapboxManeuverApi]. There is boiler plate
 * code for establishing basic navigation and a route simulator is used. The example assumes
 * that LOCATION permission has already been granted.
 *
 * The code specifically related to the maneuver component is commented in order to call
 * attention to its usage. Long press anywhere on the map to set a destination and trigger
 * navigation.
 */
class StatusActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var binding: LayoutActivityStatusBinding
    private lateinit var locationComponent: LocationComponentPlugin
    private var isNavigating = false

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * The data in the view is formatted by default mapbox distance formatting implementation.
     */
    private val distanceFormatter: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(this).build()
    }

    /**
     * The [MapboxManeuverApi] consumes route progress and banner instructions data
     * and produces trip related data that is consumed by the [MapboxManeuverView] in the layout.
     */
    private val maneuverApi: MapboxManeuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatter))
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder().build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(RouteArrowOptions.Builder(this).build())
    }

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val roadShieldCallback = RouteShieldCallback { shieldResult ->
        binding.maneuverView.renderManeuverWith(shieldResult)
    }

    private val locationObserver = object : LocationObserver {

        override fun onNewRawLocation(rawLocation: Location) {}

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            if (isNavigating) {
                updateCamera(locationMatcherResult.enhancedLocation)
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        renderManeuvers(maneuvers)

        routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
            routeArrowView.renderManeuverUpdate(mapboxMap.getStyle()!!, this)
        }
    }

    private val routesObserver = RoutesObserver { result ->
        if (result.routes.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                routeLineApi.setRoutes(
                    listOf(RouteLine(result.routes[0], null))
                ).apply {
                    routeLineView.renderRouteDrawData(mapboxMap.getStyle()!!, this)
                }
            }
            isNavigating = true
            startSimulation(result.routes[0])

            val maneuvers = maneuverApi.getManeuvers(result.routes.first())
            renderManeuvers(maneuvers)
        }
    }

    private fun renderManeuvers(maneuvers: Expected<ManeuverError, List<Maneuver>>) {
        maneuvers.onError { error ->
            LoggerProvider.logger.e(Tag("MbxManeuverActivity"), Message(error.errorMessage!!))
        }
        maneuvers.onValue {
            if (binding.maneuverView.visibility == INVISIBLE) {
                binding.maneuverView.visibility = VISIBLE
            }
            binding.maneuverView.renderManeuvers(maneuvers)
            maneuverApi.getRoadShields(
                DirectionsCriteria.PROFILE_DEFAULT_USER,
                NAVIGATION_DAY_STYLE_ID,
                getMapboxAccessToken("mapbox_access_token"),
                it,
                roadShieldCallback
            )
        }
    }

    private fun init() {
        initNavigation()
        initStyle()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        val navigationOptions = NavigationOptions.Builder(this)
            .accessToken(getMapboxAccessToken("mapbox_access_token_signboard"))
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()
        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
        mapboxNavigation.registerLocationObserver(object : LocationObserver {
            override fun onNewRawLocation(rawLocation: Location) {
                updateCamera(rawLocation)
                navigationLocationProvider.changePosition(
                    rawLocation,
                )
                mapboxNavigation.unregisterLocationObserver(this)
            }

            override fun onNewLocationMatcherResult(
                locationMatcherResult: LocationMatcherResult,
            ) {
                //
            }
        })
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxReplayer.pushRealLocation(this, 0.0)
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(NAVIGATION_DAY_STYLE) { style ->
            routeLineView.initializeLayers(style)
            binding.mapView.gestures.addOnMapLongClickListener(this)
        }
    }

    private fun getMapboxAccessToken(resourceName: String): String {
        val tokenResId = resources
            .getIdentifier(resourceName, "string", packageName)
        return if (tokenResId != 0) {
            getString(tokenResId)
        } else {
            getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
        }
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun findRoute(origin: Point, destination: Point) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinates(
                origin = origin,
                destination = destination
            )
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setRoutes(routes)
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

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        binding.mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .pitch(45.0)
                .zoom(17.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        init()
    }

    override fun onStart() {
        super.onStart()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        maneuverApi.cancel()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
    }

    //endregion

    //region Actions

    override fun onMapLongClick(point: Point): Boolean {
        ifNonNull(navigationLocationProvider.lastLocation) { currentLocation ->

            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )

            findRoute(originPoint, point)
        }
        return false
    }

    private val testStatuses by lazy {
        mutableListOf<Status>(
            Status(message = "3 sec status", duration = 3000),
            Status(message = "2 sec status", duration = 2000),
            Status(message = "1 sec status", duration = 1000),
            Status(message = "sticky"),
            Status(message = "sticky + no anim + spinner", animated = false, spinner = true),
            Status(
                message = "sticky + no anim + icon",
                animated = false,
                icon = R.drawable.ic_layers_24dp,
            ),
        )
    }

    fun onShowStatusClick(view: View) {
        val nextStatus = testStatuses.removeFirst()
        testStatuses.add(nextStatus) // put it back at the end of queue
        binding.statusView.render(nextStatus)
        binding.customStatusView.render(nextStatus)
    }

    fun onHideStatusClick(view: View) {
        binding.statusView.cancel()
        binding.customStatusView.cancel()
    }

    //endregion
}
