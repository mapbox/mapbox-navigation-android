package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.FillLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityBuildingHighlightBinding
import com.mapbox.navigation.examples.core.waypoints.WaypointsController
import com.mapbox.navigation.ui.maps.arrival.api.BuildingHighlightObserver
import com.mapbox.navigation.ui.maps.arrival.api.MapboxBuildingArrivalApi
import com.mapbox.navigation.ui.maps.arrival.api.MapboxBuildingHighlightApi
import com.mapbox.navigation.ui.maps.arrival.model.MapboxBuildingHighlightOptions
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.utils.internal.LoggerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This activity demonstrates the [MapboxBuildingHighlightApi] and the [MapboxBuildingArrivalApi].
 *
 * 1. Tap on map to highlight buildings
 * 2. Arrive at a destination and highlight buildings
 */
class MapboxBuildingHighlightActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapCamera: CameraAnimationsPlugin
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var binding: LayoutActivityBuildingHighlightBinding
    private lateinit var locationComponent: LocationComponentPlugin

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()
    private val waypointsController = WaypointsController()

    /**
     * There are two available apis for highlighting buildings. The MapboxBuildingHighlightApi
     * allows you to select buildings. When running this example, try tapping on a building
     * to see it highlight.
     *
     * The MapboxBuildingArrivalApi will automatically highlight a building when the navigation
     * has arrived.
     */
    private val buildingsArrivalApi = MapboxBuildingArrivalApi()
    private lateinit var buildingHighlightApi: MapboxBuildingHighlightApi

    /**
     * Access the current state of the extruded buildings layer.
     */
    private var extrudeBuildings: Boolean = false

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

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
            routeArrowView.renderManeuverUpdate(mapboxMap.getStyle()!!, this)
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {}
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            navigationLocationProvider.changePosition(
                enhancedLocation,
                keyPoints,
            )
            updateCamera(enhancedLocation)
        }
    }

    private val routesObserver = RoutesObserver { routes ->
        if (routes.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                routeLineApi.setRoutes(
                    listOf(RouteLine(routes[0], null))
                ).apply {
                    routeLineView.renderRouteDrawData(mapboxMap.getStyle()!!, this)
                }
            }
            startSimulation(routes[0])
        }
    }

    private fun init() {
        initNavigation()
        initStyle()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this@MapboxBuildingHighlightActivity)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()

        /**
         * Try attaching building highlights to the arrival experience.
         * At any point after constructing MapboxNavigation, enable the building arrival api
         * It is disabled by default, and should be disabled at some point after enabling.
         */
        buildingsArrivalApi.enable(mapboxNavigation)
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            MAPBOX_STREETS
        ) { style ->
            binding.mapView.gestures.addOnMapLongClickListener(this)

            /**
             * Showing all buildings is disabled by default. But this function
             * allows you to show all buildings in 3D with the highlighted one.
             */
            binding.toggleBuildings.setOnCheckedChangeListener { _, isChecked ->
                extrudeBuildings(isChecked)
            }

            /**
             * Try attaching to the map click listener, highlight the building the
             * user has selected. This observer will not influence the callback from
             * [MapboxBuildingArrivalApi.registerBuildingHighlightObserver].
             */
            binding.mapView.gestures.addOnMapClickListener { point ->
                buildingHighlightApi.highlightBuilding(point, buildingHighlightObserver)
                false
            }
        }
    }

    private val buildingHighlightObserver = BuildingHighlightObserver { features ->
        updateFillBuildingsLayer(features)
    }

    /**
     * Example map layer to highlight all buildings.
     */
    private fun updateFillBuildingsLayer(features: List<QueriedFeature>) {
        val style = mapboxMap.getStyle() ?: return
        val layerId = EXTRUDE_BUILDING_LAYER_ID
        if (!extrudeBuildings) {
            style.removeStyleLayer(layerId)
            return
        }
        val buildingFillColorExpression = buildingFillColor(style) ?: return

        val ids = features.mapNotNull { it.feature.id()?.toLong() }
        val notSelectedBuildings = Expression.not(
            Expression.inExpression(Expression.id(), literal(ids))
        )
        if (!style.styleLayerExists(layerId)) {
            style.addLayer(
                FillExtrusionLayer(layerId, MapboxBuildingHighlightApi.COMPOSITE_SOURCE_ID)
                    .sourceLayer(MapboxBuildingHighlightApi.BUILDING_LAYER_ID)
                    .filter(notSelectedBuildings)
                    .fillExtrusionColor(buildingFillColorExpression)
                    .fillExtrusionOpacity(literal(0.6))
                    .fillExtrusionBase(Expression.get("min-height"))
                    .fillExtrusionHeight(buildingHighlightApi.buildingHeightExpression)
            )
        } else {
            style.getLayerAs<FillExtrusionLayer>(layerId)
                .filter(notSelectedBuildings)
        }
    }

    private fun buildingFillColor(style: Style): Expression? {
        val buildingLayerId = MapboxBuildingHighlightApi.BUILDING_LAYER_ID
        return when (val buildingLayer = style.getLayer(buildingLayerId)) {
            is FillLayer -> buildingLayer.fillColorAsExpression
            else -> {
                LoggerProvider.logger.e(
                    msg = Message("$buildingLayerId has unsupported type $buildingLayer")
                )
                null
            }
        }
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun getMapCamera(): CameraAnimationsPlugin {
        return binding.mapView.camera
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        mapCamera.easeTo(
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

    private fun findRoute(origin: Point) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinatesList(waypointsController.coordinates(origin))
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LayoutActivityBuildingHighlightBinding.inflate(layoutInflater)

        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()

        /**
         * Initialize the building highlight at any point after retrieving the MapboxMap.
         * Apply custom options when creating the api.
         */
        buildingHighlightApi = MapboxBuildingHighlightApi(
            mapboxMap,
            MapboxBuildingHighlightOptions.Builder()
                .fillExtrusionColor(Color.RED)
                .build()
        )

        /**
         * After initializing an api to highligh buildings, give the handle to the
         * building arrival api. It will select a building upon arrival
         */
        buildingsArrivalApi.buildingHighlightApi(buildingHighlightApi)

        locationComponent = binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        mapCamera = getMapCamera()

        init()
    }

    /**
     * Show all buildings on the map.
     */
    private fun extrudeBuildings(extrudeBuildings: Boolean) {
        this.extrudeBuildings = extrudeBuildings
        updateFillBuildingsLayer(buildingHighlightApi.highlightedBuildings)
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)

            /**
             * In order to remove the highlighted building from the extruded
             * buildings. You can register an observer to update your fill layer.
             */
            buildingsArrivalApi.registerBuildingHighlightObserver(buildingHighlightObserver)

            /**
             * At any point after constructing a MapboxNavigation object, you can enable
             * the buildings arrival api.
             */
            buildingsArrivalApi.enable(mapboxNavigation)
        }
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)

        /**
         * Removes the observer.
         */
        buildingsArrivalApi.unregisterBuildingHighlightObserver(buildingHighlightObserver)

        /**
         * Pick your lifecycle to disable the arrival api. MapboxNavigation.onDestroy will
         * automatically unregister the api, but will not clear the style data.
         */
        buildingsArrivalApi.disable()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onMapLongClick(point: Point): Boolean {
        val currentLocation = navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            waypointsController.add(point)
            val origin = Point.fromLngLat(currentLocation.longitude, currentLocation.latitude)
            findRoute(origin)
        }
        return false
    }

    private companion object {
        private const val EXTRUDE_BUILDING_LAYER_ID = "mapbox-building-extrude-layer"
    }
}
