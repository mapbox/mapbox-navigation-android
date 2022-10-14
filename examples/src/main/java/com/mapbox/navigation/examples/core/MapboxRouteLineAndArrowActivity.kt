package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityRoutelineExampleBinding
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancelChildren
import java.lang.ref.WeakReference

/**
 * This class demonstrates the usage of the route line and route arrow API's. There is
 * boiler plate code for a basic navigation experience. The turn by turn navigation is simulated.
 * The route line and arrow specific code is indicated with inline comments.
 */
class MapboxRouteLineAndArrowActivity : AppCompatActivity(), OnMapLongClickListener {
    private val routeClickPadding = Utils.dpToPx(30f)
    private val ONE_HUNDRED_MILLISECONDS = 100
    private val mapboxReplayer = MapboxReplayer()
    private val replayRouteMapper = ReplayRouteMapper()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private var trafficGradientSoft = false
    private val jobControl = InternalJobControlFactory.createDefaultScopeJobControl()

    private val viewBinding: LayoutActivityRoutelineExampleBinding by lazy {
        LayoutActivityRoutelineExampleBinding.inflate(layoutInflater)
    }

    private val mapboxMap: MapboxMap by lazy {
        viewBinding.mapView.getMapboxMap()
    }

    private val navigationLocationProvider by lazy {
        NavigationLocationProvider()
    }

    private val locationComponent by lazy {
        viewBinding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    private val mapboxNavigation by lazy {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
    }

    private val mapCamera by lazy {
        viewBinding.mapView.camera
    }

    // RouteLine: Route line related colors can be customized via the RouteLineColorResources.
    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder()
            .routeLineTraveledColor(Color.LTGRAY)
            .routeLineTraveledCasingColor(Color.GRAY)
            .build()
    }

    // RouteLine: Various route line related options can be customized here including applying
    // route line color customizations. If using the default colors the RouteLineColorResources
    // does not need to be set as seen here, the defaults will be used internally by the builder.
    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    // RouteLine: Additional route line options are available through the MapboxRouteLineOptions.
    // Notice here the withRouteLineBelowLayerId option. The map is made up of layers. In this
    // case the route line will be placed below the "road-label" layer which is a good default
    // for the most common Mapbox navigation related maps. You should consider if this should be
    // changed for your use case especially if you are using a custom map style.
    //
    // Also noteworthy is the 'withVanishingRouteLineEnabled' option. This feature will change
    // the color of the route line behind the puck during navigation. The color can be customized
    // using the RouteLineColorResources::routeLineTraveledColor and
    // RouteLineColorResources::routeLineTraveledCasingColor options. The color options support
    // an alpha value to render the line transparent which is the default.
    //
    // To use the vanishing route line feature it is also necessary to register an
    // OnIndicatorPositionChangedListener and a RouteProgressObserver. There may be reasons to use
    // a RouteProgressObserver even if you are not using the vanishing route line feature.
    // The OnIndicatorPositionChangedListener is only useful and required when enabling the
    // vanishing route line feature.
    //
    // Examples are below.
    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .withVanishingRouteLineEnabled(true)
            .displaySoftGradientForTraffic(trafficGradientSoft)
            .build()
    }

    // RouteLine: This class is responsible for rendering route line related mutations generated
    // by the MapboxRouteLineApi class.
    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    // RouteLine: This class is responsible for generating route line related data which must be
    // rendered by the MapboxRouteLineView class in order to visualize the route line on the map.
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    // RouteArrow: This class is responsible for generating data related to maneuver arrows. The
    // data generated must be rendered by the MapboxRouteArrowView in order to apply mutations to
    // the map.
    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    // RouteArrow: Customization of the maneuver arrow(s) can be done using the
    // RouteArrowOptions. Here the above layer ID is used to determine where in the map layer
    // stack the arrows appear. Above the layer of the route traffic line is being used here. Your
    // use case may necessitate adjusting this to a different layer position.
    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }

    // RouteArrow: This class is responsible for rendering the arrow related mutations generated
    // by the MapboxRouteArrowApi class.
    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(routeArrowOptions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        init()
    }

    private fun init() {
        initGradientSelector()
        initNavigation()
        initStyle()
        initListeners()
        locationComponent.locationPuck = LocationPuck2D(
            null,
            ContextCompat.getDrawable(
                this@MapboxRouteLineAndArrowActivity,
                R.drawable.custom_user_puck_icon
            ),
            null,
            null
        )
    }

    private fun initGradientSelector() {
        viewBinding.gradientOptionHard.setOnClickListener {
            trafficGradientSoft = false
        }
        viewBinding.gradientOptionSoft.setOnClickListener {
            trafficGradientSoft = true
        }
    }

    // RouteLine: This is one way to keep the route(s) appearing on the map in sync with
    // MapboxNavigation. When this observer is called the route data is used to draw route(s)
    // on the map.
    private val routesObserver: RoutesObserver = RoutesObserver { result ->
        // RouteLine: wrap the DirectionRoute objects and pass them
        // to the MapboxRouteLineApi to generate the data necessary to draw the route(s)
        // on the map.
        val routeLines = result.routes.map { RouteLine(it, null) }
        routeLineApi.setRoutes(
            routeLines
        ) { value ->
            // RouteLine: The MapboxRouteLineView expects a non-null reference to the map style.
            // the data generated by the call to the MapboxRouteLineApi above must be rendered
            // by the MapboxRouteLineView in order to visualize the changes on the map.
            mapboxMap.getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    // RouteLine: This listener is necessary only when enabling the vanishing route line feature
    // which changes the color of the route line behind the puck during navigation. If this
    // option is set to `false` (the default) in MapboxRouteLineOptions then it is not necessary
    // to use this listener.
    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->

        //todo remove this
        // RouteLine: This line is only necessary if the vanishing route line feature
        // is enabled.
        // routeLineApi.updateWithRouteProgress(routeProgress) { result ->
        //     mapboxMap.getStyle()?.apply {
        //         routeLineView.renderRouteLineUpdate(this, result)
        //     }
        // }

        routeProgress.currentLegProgress?.currentStepProgress?.step?.apply {
            val points = routeProgress.route.stepGeometryToPoints(this)
            addPointToPixelMapPoints(points)
        }

        routeLineApi.deleteMeGetTreePoints().apply {
            addTreePoints(this)
        }

        // RouteArrow: The next maneuver arrows are driven by route progress events.
        // Generate the next maneuver arrow update data and pass it to the view class
        // to visualize the updates on the map.
        val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeArrowView.renderManeuverUpdate(this, arrowUpdate)
        }
    }

    // RouteLine: Below is a demonstration of selecting different routes. On a map click, a call
    // will be made to look for a route line on the map based on the map touch point. If a route is
    // found and it is not already the primary route, the selected route will designated the primary
    // route and MapboxNavigation will be updated.
    private val mapClickListener = OnMapClickListener { point ->
        mapboxMap.getStyle()?.apply {
            // Since this listener is reacting to all map touches, if the primary and alternative
            // routes aren't visible it's assumed the touch isn't related to selecting an
            // alternative route.
            val primaryLineVisibility = routeLineView.getPrimaryRouteVisibility(this)
            val alternativeRouteLinesVisibility = routeLineView.getAlternativeRoutesVisibility(this)
            if (
                primaryLineVisibility == Visibility.VISIBLE &&
                alternativeRouteLinesVisibility == Visibility.VISIBLE
            ) {
                routeLineApi.findClosestRoute(
                    point,
                    mapboxMap,
                    routeClickPadding
                ) { result ->
                    result.onValue { value ->
                        if (value.route != routeLineApi.getPrimaryRoute()) {
                            val reOrderedRoutes = routeLineApi.getRoutes()
                                .filter { it != value.route }
                                .toMutableList()
                                .also {
                                    it.add(0, value.route)
                                }
                            mapboxNavigation.setRoutes(reOrderedRoutes)
                        }
                    }
                }
            }
        }

        false
    }

    private fun initNavigation() {
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)

        // The lines below are related to the navigation simulator.
        mapboxReplayer.pushRealLocation(this, 0.0)
        //mapboxReplayer.playbackSpeed(1.5)
        mapboxReplayer.play()
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            updateCamera(locationMatcherResult.enhancedLocation)
        }
    }

    private fun updateCamera(location: Location) {
        if (cameraUpdatesAllowed) {
            val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
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
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE,
            { style: Style ->
                // Get the last known location and move the map to that location.
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                    locationEngineCallback
                )
                viewBinding.mapView.gestures.addOnMapLongClickListener(this)

                initPointLayer(style)
                initTreePointLayer(style)
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.e(
                        MapboxRouteLineAndArrowActivity::class.java.simpleName,
                        "Error loading map - error type: " +
                            "${eventData.type}, message: ${eventData.message}"
                    )
                }
            }
        )
    }

    override fun onMapLongClick(point: Point): Boolean {
        vibrate()
        viewBinding.startNavigation.visibility = View.GONE
        viewBinding.optionTrafficGradient.visibility = View.GONE
        val currentLocation = navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, point)
            viewBinding.routeLoadingProgressBar.visibility = View.VISIBLE
        }
        return false
    }

    fun findRoute(origin: Point?, destination: Point?) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private val routesReqCallback: RouterCallback = object : RouterCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
            mapboxNavigation.setRoutes(listOf(getRoute()))
            //mapboxNavigation.setRoutes(routes)
            if (routes.isNotEmpty()) {
                viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
                viewBinding.startNavigation.visibility = View.VISIBLE
            }
        }

        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    ONE_HUNDRED_MILLISECONDS.toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(ONE_HUNDRED_MILLISECONDS.toLong())
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        viewBinding.startNavigation.setOnClickListener {
            val route = mapboxNavigation.getRoutes().firstOrNull()
            if (route != null) {
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.startTripSession()
                viewBinding.startNavigation.visibility = View.INVISIBLE
                locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)

                // RouteLine: Hiding the alternative routes when navigation starts.
                mapboxMap.getStyle()?.apply {
                    routeLineView.hideAlternativeRoutes(this)
                }

                startSimulation(route)
            }
        }

        viewBinding.btnTemp.setOnClickListener {
            cameraUpdatesAllowed = !cameraUpdatesAllowed
        }
        viewBinding.mapView.gestures.addOnMapClickListener(mapClickListener)
    }
    var cameraUpdatesAllowed = true

    // Starts the navigation simulator
    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData: List<ReplayEventBase> = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
    }

    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
    }

    override fun onStop() {
        super.onStop()
        jobControl.job.cancelChildren()
        locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
    }

    private val locationEngineCallback = MyLocationEngineCallback(this)

    private class MyLocationEngineCallback(activity: MapboxRouteLineAndArrowActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef: WeakReference<MapboxRouteLineAndArrowActivity> by lazy {
            WeakReference(activity)
        }

        override fun onSuccess(result: LocationEngineResult?) {
            val location = result?.lastLocation
            val activity = activityRef.get()
            if (location != null && activity != null) {
                val point = Point.fromLngLat(location.longitude, location.latitude)
                val cameraOptions = CameraOptions.Builder().center(point).zoom(13.0).build()
                activity.mapboxMap.setCamera(cameraOptions)
                activity.navigationLocationProvider.changePosition(location, listOf(), null, null)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private val LINE_END_LAYER_ID = "DRAW_UTIL_LINE_END_LAYER_ID"
    private val LINE_END_SOURCE_ID = "DRAW_UTIL_LINE_END_SOURCE_ID"
    private fun initPointLayer(style: Style) {
        if (!style.styleSourceExists(LINE_END_SOURCE_ID)) {
            geoJsonSource(LINE_END_SOURCE_ID) {}.bindTo(style)
        }

        if (!style.styleLayerExists(LINE_END_LAYER_ID)) {
            CircleLayer(LINE_END_LAYER_ID, LINE_END_SOURCE_ID)
                .circleRadius(5.0)
                .circleOpacity(1.0)
                .circleColor(Color.BLACK)
                .bindTo(style)
        }
    }

    private val TREE_LINE_LAYER_ID = "TREE_LINE_LAYER_ID"
    private val TREE_LINE_SOURCE_ID = "TREE_LINE_SOURCE_ID"
    private fun initTreePointLayer(style: Style) {
        if (!style.styleSourceExists(TREE_LINE_SOURCE_ID)) {
            geoJsonSource(TREE_LINE_SOURCE_ID) {}.bindTo(style)
        }

        if (!style.styleLayerExists(TREE_LINE_LAYER_ID)) {
            CircleLayer(TREE_LINE_LAYER_ID, TREE_LINE_SOURCE_ID)
                .circleRadius(2.0)
                .circleOpacity(.75)
                .circleColor(Color.MAGENTA)
                .bindTo(style)
        }
    }

    // todo remove this
    private fun addPointToPixelMapPoints(points: List<Point>) {
        val features = points.map { Feature.fromGeometry(it) }

        (mapboxMap.getStyle()!!.getSource(LINE_END_SOURCE_ID) as GeoJsonSource).apply {
            this.featureCollection(FeatureCollection.fromFeatures(features))
        }
    }

    private fun addTreePoints(points: List<Point>) {
        val features = points.map { Feature.fromGeometry(it) }

        (mapboxMap.getStyle()!!.getSource(TREE_LINE_SOURCE_ID) as GeoJsonSource).apply {
            this.featureCollection(FeatureCollection.fromFeatures(features))
        }
    }

    fun getRoute(): DirectionsRoute {
        val routeJson = "{\"routeIndex\":\"0\",\"distance\":728.195,\"duration\":178.495,\"geometry\":\"yxh`vAtyg~hFyXQiZO}SKsp@YEnd@I|_@|]HrTJxf@B~FgAPoj@UoK{@iJu@oIB_c@?uE@gM?QDoV?{@?gHB_V\",\"weight\":241.413,\"weight_name\":\"auto\",\"legs\":[{\"weight\":241.413,\"via_waypoints\":[],\"distance\":728.195,\"duration\":178.495,\"summary\":\"Main Street, West McLoughlin Boulevard\",\"admins\":[{\"iso_3166_1\":\"US\",\"iso_3166_1_alpha3\":\"USA\"}],\"steps\":[{\"distance\":220.325,\"duration\":29.108,\"geometry\":\"yxh`vAtyg~hFyXQiZO}SKsp@Y\",\"name\":\"Main Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671531,45.634461],\"bearing_before\":0.0,\"bearing_after\":1.0,\"instruction\":\"Drive north on Main Street.\",\"type\":\"depart\"},\"driving_side\":\"right\",\"weight\":35.461,\"intersections\":[{\"duration\":7.5,\"weight\":9,\"location\":[-122.671531,45.634461],\"bearings\":[1],\"entry\":[true],\"out\":0,\"geometry_index\":0,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":9.34,\"turn_duration\":2.019,\"turn_weight\":1.5,\"duration\":8.552,\"location\":[-122.671522,45.634874],\"bearings\":[1,82,181,259],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":1,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":3.719,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":4.94,\"location\":[-122.671514,45.635311],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":2,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"turn_weight\":1,\"turn_duration\":0.019,\"location\":[-122.671508,45.635646],\"bearings\":[1,92,181,249],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":3,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}}]},{\"distance\":87.726,\"duration\":31.082,\"geometry\":\"otl`vAlwg~hFEnd@I|_@\",\"name\":\"West 20th Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671495,45.63644],\"bearing_before\":1.0,\"bearing_after\":270.0,\"instruction\":\"Turn left onto West 20th Street.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":45.681,\"intersections\":[{\"duration\":19.722,\"turn_weight\":12.5,\"turn_duration\":5.622,\"weight\":29.773,\"location\":[-122.671495,45.63644],\"bearings\":[1,104,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":3,\"geometry_index\":4,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"turn_weight\":2,\"turn_duration\":0.007,\"location\":[-122.672095,45.636443],\"bearings\":[1,90,182,271],\"entry\":[true,false,true,true],\"in\":1,\"out\":3,\"geometry_index\":5,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}}]},{\"distance\":179.057,\"duration\":68.349,\"geometry\":\"_ul`vAz}i~hF|]HrTJxf@B~FgA\",\"name\":\"Washington Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.672622,45.636448],\"bearing_before\":271.0,\"bearing_after\":180.0,\"instruction\":\"Turn left onto Washington Street.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":88.24,\"intersections\":[{\"duration\":30.372,\"turn_weight\":10,\"turn_duration\":5.622,\"weight\":39.7,\"location\":[-122.672622,45.636448],\"bearings\":[1,91,180,270],\"entry\":[true,false,true,true],\"in\":1,\"out\":2,\"geometry_index\":6,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"duration\":17.558,\"turn_weight\":1,\"turn_duration\":0.008,\"weight\":22.06,\"location\":[-122.672627,45.635953],\"bearings\":[0,92,181],\"entry\":[false,true,true],\"in\":0,\"out\":2,\"geometry_index\":7,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"turn_weight\":2,\"turn_duration\":0.019,\"location\":[-122.672633,45.635607],\"bearings\":[1,92,180,270],\"entry\":[false,true,true,true],\"in\":0,\"out\":2,\"geometry_index\":8,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}}]},{\"distance\":241.086,\"duration\":49.955,\"geometry\":\"spi`vAl|i~hFPoj@UoK{@iJu@oIB_c@?uE@gM?QDoV?{@?gHB_V\",\"name\":\"West McLoughlin Boulevard\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.672599,45.634842],\"bearing_before\":169.0,\"bearing_after\":91.0,\"instruction\":\"Turn left onto West McLoughlin Boulevard.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":72.031,\"intersections\":[{\"duration\":22.522,\"turn_weight\":12.5,\"turn_duration\":3.622,\"weight\":35.18,\"location\":[-122.672599,45.634842],\"bearings\":[91,182,270,349],\"entry\":[true,true,true,false],\"in\":3,\"out\":0,\"geometry_index\":10,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"weight\":14.931,\"turn_duration\":2.008,\"turn_weight\":3,\"duration\":11.951,\"location\":[-122.671522,45.634874],\"bearings\":[1,82,182,259],\"entry\":[true,true,true,false],\"in\":3,\"out\":1,\"geometry_index\":13,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"duration\":1.371,\"turn_weight\":0.75,\"weight\":2.396,\"location\":[-122.670778,45.634899],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":15,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.086,\"turn_weight\":0.75,\"weight\":4.453,\"location\":[-122.670671,45.634899],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":16,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"weight\":6.469,\"turn_duration\":2.008,\"turn_weight\":2,\"duration\":5.732,\"location\":[-122.670443,45.634898],\"bearings\":[1,91,182,270],\"entry\":[true,true,true,false],\"in\":3,\"out\":1,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":17,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"duration\":0.248,\"turn_weight\":0.75,\"weight\":1.048,\"location\":[-122.670058,45.634895],\"bearings\":[90,271],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":19,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":1.49,\"turn_weight\":0.75,\"weight\":2.538,\"location\":[-122.670028,45.634895],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":20,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"turn_weight\":0.75,\"location\":[-122.66988,45.634895],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":21,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"ysi`vAn{c~hF??\",\"name\":\"West McLoughlin Boulevard\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.669512,45.634893],\"bearing_before\":90.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination.\",\"type\":\"arrive\"},\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.669512,45.634893],\"bearings\":[270],\"entry\":[true],\"in\":0,\"geometry_index\":22,\"admin_index\":0}]}],\"annotation\":{\"distance\":[46.0,48.6,37.3,88.4,46.7,41.1,55.1,38.5,70.9,14.5,54.2,15.6,14.5,13.4,44.8,8.4,17.7,0.7,29.3,2.3,11.5,28.7],\"congestion_numeric\":[0,null,4,4,null,null,null,null,null,null,0,0,0,4,4,4,4,1,1,1,1,1]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":\"-122.6715815,45.6344615;-122.6716338,45.6364896;-122.6726954,45.6364085;-122.672707,45.6348712;-122.6695105,45.6350132\",\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion_numeric,distance\",\"voice_instructions\":false,\"banner_instructions\":false,\"waypoints\":\"0;4\"},\"requestUuid\":\"mapmatching\"}"
        //val routeJson = "{\"country_crossed\":false,\"weight_typical\":373.949,\"routeIndex\":\"0\",\"distance\":2344.955,\"duration\":275.465,\"duration_typical\":275.465,\"geometry\":\"kyh`vAtyg~hFgXQiZO}SKsp@Y{TOgMGaRKaLGmDAmCCkCA_DA}KGgPIu[QaEAic@UcKEmZM_DAChF?lEIh^?bDC`PEtb@Cf[AzEAfEE|RSb{@?rc@YdoAInbAGlSMfaAGpSM``@AtCEnMKl\\\\oMMcRO}@Aws@o@qD?{RKgZKwNGmRGkICg`@Mg_@UgJGuJGkFE}VKyUM_WGmCA}D?{DEa_@QkBAsSEwOKyLMqNQaTIWzeA\",\"weight\":373.949,\"weight_name\":\"auto\",\"legs\":[{\"weight_typical\":373.949,\"weight\":373.949,\"via_waypoints\":[],\"distance\":2344.955,\"duration\":275.465,\"duration_typical\":275.465,\"summary\":\"West Fourth Plain Boulevard, Kauffman Avenue\",\"admins\":[{\"iso_3166_1\":\"US\",\"iso_3166_1_alpha3\":\"USA\"}],\"steps\":[{\"weight_typical\":111.941,\"distance\":634.918,\"duration\":86.844,\"duration_typical\":86.844,\"speedLimitUnit\":\"mph\",\"speedLimitSign\":\"mutcd\",\"geometry\":\"kyh`vAtyg~hFgXQiZO}SKsp@Y{TOgMGaRKaLGmDAmCCkCA_DA}KGgPIu[QaEAic@UcKEmZM_DA\",\"name\":\"Main Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671531,45.63447],\"bearing_before\":0.0,\"bearing_after\":1.0,\"instruction\":\"Drive north on Main Street.\",\"type\":\"depart\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":634.918,\"announcement\":\"Drive north on Main Street for a half mile.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eDrive north on \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eMain Street\\u003c/say-as\\u003e for a half mile.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":402.336,\"announcement\":\"In a quarter mile, Turn left onto West Fourth Plain Boulevard.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, Turn left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eWest Fourth Plain Boulevard\\u003c/say-as\\u003e.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":42.667,\"announcement\":\"Turn left.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":634.918,\"primary\":{\"text\":\"West Fourth Plain Boulevard\",\"components\":[{\"text\":\"West Fourth Plain Boulevard\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":402.336,\"primary\":{\"text\":\"West Fourth Plain Boulevard\",\"components\":[{\"text\":\"West Fourth Plain Boulevard\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"left\"},\"sub\":{\"text\":\"\",\"components\":[{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"left\"],\"active\":true,\"active_direction\":\"left\"},{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"straight\"],\"active\":false},{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"right\"],\"active\":false}]}}],\"driving_side\":\"right\",\"weight\":111.941,\"intersections\":[{\"duration\":7.315,\"weight\":8.778,\"location\":[-122.671531,45.63447],\"bearings\":[1],\"entry\":[true],\"out\":0,\"geometry_index\":0,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":9.34,\"turn_duration\":2.019,\"turn_weight\":1.5,\"duration\":8.552,\"location\":[-122.671522,45.634874],\"bearings\":[1,82,181,259],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":1,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":3.937,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":5.201,\"location\":[-122.671514,45.635311],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":2,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":9.919,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":12.88,\"location\":[-122.671508,45.635646],\"bearings\":[1,92,181,249],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":3,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":4.031,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":5.914,\"location\":[-122.671495,45.63644],\"bearings\":[1,104,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":4,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.832,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":3.945,\"location\":[-122.671487,45.63679],\"bearings\":[1,92,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":5,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.844,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":5.186,\"location\":[-122.671483,45.637018],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":6,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.874,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":3.998,\"location\":[-122.671477,45.637323],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":7,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.254,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":3.237,\"location\":[-122.671473,45.637532],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":8,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":2.918,\"turn_duration\":2.019,\"turn_weight\":0.5,\"duration\":3.993,\"location\":[-122.67147,45.63769],\"bearings\":[1,92,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":10,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":2.779,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":3.881,\"location\":[-122.671468,45.63784],\"bearings\":[1,181,267],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":12,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.619,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":5.41,\"location\":[-122.671464,45.638047],\"bearings\":[1,92,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":13,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":7.363,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":9.496,\"location\":[-122.671459,45.638323],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":14,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":10.819,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":14.23,\"location\":[-122.67145,45.638782],\"bearings\":[1,75,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":15,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.75,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":4.346,\"location\":[-122.671438,45.63946],\"bearings\":[1,92,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":17,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"turn_weight\":1,\"turn_duration\":0.019,\"location\":[-122.671435,45.639654],\"bearings\":[1,90,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":18,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}}]},{\"weight_typical\":104.434,\"distance\":744.28,\"duration\":79.063,\"duration_typical\":79.063,\"speedLimitUnit\":\"mph\",\"speedLimitSign\":\"mutcd\",\"geometry\":\"y}s`vAdsg~hFChF?lEIh^?bDC`PEtb@Cf[AzEAfEE|RSb{@?rc@YdoAInbAGlSMfaAGpSM``@AtCEnMKl\\\\\",\"name\":\"West Fourth Plain Boulevard\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671427,45.640173],\"bearing_before\":1.0,\"bearing_after\":271.0,\"instruction\":\"Turn left onto West Fourth Plain Boulevard.\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":730.946,\"announcement\":\"Continue for a half mile.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eContinue for a half mile.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":402.336,\"announcement\":\"In a quarter mile, Turn right onto Kauffman Avenue.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, Turn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eKauffman Avenue\\u003c/say-as\\u003e.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":66.667,\"announcement\":\"Turn right onto Kauffman Avenue.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eKauffman Avenue\\u003c/say-as\\u003e.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":744.28,\"primary\":{\"text\":\"Kauffman Avenue\",\"components\":[{\"text\":\"Kauffman Avenue\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":402.336,\"primary\":{\"text\":\"Kauffman Avenue\",\"components\":[{\"text\":\"Kauffman Avenue\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"right\"},\"sub\":{\"text\":\"\",\"components\":[{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"left\"],\"active\":false},{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"straight\",\"right\"],\"active\":true,\"active_direction\":\"right\"}]}}],\"driving_side\":\"right\",\"weight\":104.434,\"intersections\":[{\"weight\":12.272,\"turn_duration\":7.622,\"turn_weight\":10,\"duration\":9.476,\"location\":[-122.671427,45.640173],\"bearings\":[15,90,181,271],\"entry\":[true,true,false,true],\"in\":2,\"out\":3,\"lanes\":[{\"valid\":true,\"active\":true,\"valid_indication\":\"left\",\"indications\":[\"left\"]},{\"valid\":false,\"active\":false,\"indications\":[\"straight\"]},{\"valid\":false,\"active\":false,\"indications\":[\"right\"]}],\"geometry_index\":20,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":4.93,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":6.514,\"location\":[-122.671647,45.640175],\"bearings\":[1,91,271],\"entry\":[true,false,true],\"in\":1,\"out\":2,\"geometry_index\":22,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.821,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":3.93,\"location\":[-122.67223,45.64018],\"bearings\":[1,91,271],\"entry\":[true,false,true],\"in\":1,\"out\":2,\"geometry_index\":24,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":5.867,\"turn_weight\":0.5,\"weight\":7.687,\"location\":[-122.672503,45.640182],\"bearings\":[91,270],\"entry\":[false,true],\"in\":0,\"out\":1,\"geometry_index\":25,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":5.867,\"turn_weight\":0.5,\"weight\":7.687,\"location\":[-122.673074,45.640185],\"bearings\":[90,270],\"entry\":[false,true],\"in\":0,\"out\":1,\"geometry_index\":26,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":5.138,\"turn_duration\":2.021,\"turn_weight\":1.5,\"duration\":4.991,\"location\":[-122.673636,45.640188],\"bearings\":[91,179,271,359],\"entry\":[false,true,true,true],\"in\":0,\"out\":2,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":28,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":6.75,\"turn_weight\":0.5,\"weight\":8.769,\"location\":[-122.674055,45.640192],\"bearings\":[91,271],\"entry\":[false,true],\"in\":0,\"out\":1,\"geometry_index\":30,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.784,\"turn_weight\":1,\"turn_duration\":0.021,\"weight\":5.61,\"location\":[-122.675017,45.640202],\"bearings\":[1,91,191,270],\"entry\":[true,false,true,true],\"in\":1,\"out\":3,\"geometry_index\":31,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":8.007,\"turn_weight\":0.5,\"turn_duration\":0.007,\"weight\":10.1,\"location\":[-122.675603,45.640202],\"bearings\":[0,90,271],\"entry\":[true,false,true],\"in\":1,\"out\":2,\"geometry_index\":32,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":6.455,\"turn_weight\":1.5,\"turn_duration\":0.021,\"weight\":9.221,\"location\":[-122.676886,45.640215],\"bearings\":[1,91,180,270],\"entry\":[true,false,true,true],\"in\":1,\"out\":3,\"geometry_index\":33,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.844,\"turn_weight\":0.5,\"turn_duration\":0.007,\"weight\":2.704,\"location\":[-122.677966,45.64022],\"bearings\":[90,182,271],\"entry\":[false,true,true],\"in\":0,\"out\":2,\"geometry_index\":34,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":6.246,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":7.97,\"location\":[-122.678293,45.640224],\"bearings\":[1,91,271],\"entry\":[true,false,true],\"in\":1,\"out\":2,\"geometry_index\":35,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.971,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":2.84,\"location\":[-122.679353,45.640231],\"bearings\":[91,182,271],\"entry\":[false,true,true],\"in\":0,\"out\":2,\"geometry_index\":36,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.301,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":4.436,\"location\":[-122.679682,45.640235],\"bearings\":[1,91,271],\"entry\":[true,false,true],\"in\":1,\"out\":2,\"geometry_index\":37,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":0.548,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":1.132,\"location\":[-122.680211,45.640242],\"bearings\":[91,180,271],\"entry\":[false,true,true],\"in\":0,\"out\":2,\"geometry_index\":38,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.046,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":2.93,\"location\":[-122.680286,45.640243],\"bearings\":[1,91,271],\"entry\":[true,false,true],\"in\":1,\"out\":2,\"geometry_index\":39,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"turn_weight\":0.5,\"location\":[-122.680518,45.640246],\"bearings\":[91,271],\"entry\":[false,true],\"in\":0,\"out\":1,\"geometry_index\":40,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}}]},{\"weight_typical\":131.023,\"distance\":877.516,\"duration\":91.718,\"duration_typical\":91.718,\"speedLimitUnit\":\"mph\",\"speedLimitSign\":\"mutcd\",\"geometry\":\"wbt`vAxhz~hFoMMcRO}@Aws@o@qD?{RKgZKwNGmRGkICg`@Mg_@UgJGuJGkFE}VKyUM_WGmCA}D?{DEa_@QkBAsSEwOKyLMqNQaTI\",\"name\":\"Kauffman Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.680989,45.640252],\"bearing_before\":271.0,\"bearing_after\":1.0,\"instruction\":\"Turn right onto Kauffman Avenue.\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":864.182,\"announcement\":\"Continue for a half mile.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eContinue for a half mile.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":402.336,\"announcement\":\"In a quarter mile, Turn left onto West 36th Street.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, Turn left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eWest 36th Street\\u003c/say-as\\u003e.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":91.111,\"announcement\":\"Turn left onto West 36th Street. Then Your destination will be on the left.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eWest 36th Street\\u003c/say-as\\u003e. Then Your destination will be on the left.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":877.516,\"primary\":{\"text\":\"West 36th Street\",\"components\":[{\"text\":\"West 36th Street\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":131.023,\"intersections\":[{\"weight\":19.48,\"turn_duration\":4.005,\"turn_weight\":7,\"duration\":14.405,\"location\":[-122.680989,45.640252],\"bearings\":[1,91,180,270],\"entry\":[true,false,true,true],\"in\":1,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"right\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":41,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"duration\":5.119,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":6.87,\"location\":[-122.680982,45.640484],\"bearings\":[1,181,269],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":42,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":0.276,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":1.059,\"location\":[-122.680974,45.64079],\"bearings\":[1,92,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":43,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":7.069,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":9.21,\"location\":[-122.680973,45.640821],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":44,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.467,\"turn_weight\":0.75,\"turn_duration\":0.021,\"weight\":4.886,\"location\":[-122.680949,45.641665],\"bearings\":[0,92,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":45,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":5.059,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":6.798,\"location\":[-122.680943,45.642072],\"bearings\":[1,89,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":47,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.752,\"turn_weight\":1.5,\"turn_duration\":0.019,\"weight\":5.98,\"location\":[-122.680937,45.642508],\"bearings\":[1,92,181,267],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":48,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.019,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":4.35,\"location\":[-122.680933,45.64276],\"bearings\":[1,181,270],\"entry\":[true,false,false],\"in\":1,\"out\":0,\"geometry_index\":49,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":6.869,\"turn_weight\":0.75,\"turn_duration\":0.021,\"weight\":8.969,\"location\":[-122.680929,45.643071],\"bearings\":[0,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":50,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":4.294,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":5.88,\"location\":[-122.68092,45.643769],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":52,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":1.619,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":2.67,\"location\":[-122.680909,45.644285],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":53,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":2.866,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":4.166,\"location\":[-122.680905,45.644465],\"bearings\":[1,181,276],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":54,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.795,\"turn_weight\":1.5,\"turn_duration\":0.019,\"weight\":6.031,\"location\":[-122.680898,45.64477],\"bearings\":[1,90,181,269],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":56,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.804,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":5.197,\"location\":[-122.680892,45.645153],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":57,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":4.204,\"turn_weight\":2,\"turn_duration\":0.021,\"weight\":6.916,\"location\":[-122.680885,45.645518],\"bearings\":[0,90,181,265],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":58,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":0.71,\"turn_weight\":0.75,\"turn_duration\":0.007,\"weight\":1.575,\"location\":[-122.680881,45.645902],\"bearings\":[1,90,180],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":59,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":2.062,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":3.151,\"location\":[-122.68088,45.645973],\"bearings\":[1,181,269],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":60,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":5.149,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":6.778,\"location\":[-122.680877,45.646162],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":62,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":0.533,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":1.354,\"location\":[-122.680868,45.646675],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":63,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.118,\"turn_weight\":1.5,\"turn_duration\":0.021,\"weight\":5.14,\"location\":[-122.680867,45.646729],\"bearings\":[0,92,181,284],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":64,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":2.579,\"turn_weight\":0.75,\"turn_duration\":0.007,\"weight\":3.771,\"location\":[-122.680864,45.647059],\"bearings\":[1,180,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":65,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":4.819,\"turn_weight\":0.75,\"turn_duration\":0.019,\"weight\":6.39,\"location\":[-122.680858,45.647327],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":66,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"turn_weight\":0.75,\"turn_duration\":0.019,\"location\":[-122.680842,45.647797],\"bearings\":[1,92,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":68,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}}]},{\"weight_typical\":26.551,\"distance\":88.242,\"duration\":17.84,\"duration_typical\":17.84,\"speedLimitUnit\":\"mph\",\"speedLimitSign\":\"mutcd\",\"geometry\":\"kocavAh_z~hFWzeA\",\"name\":\"West 36th Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.680837,45.648134],\"bearing_before\":1.0,\"bearing_after\":271.0,\"instruction\":\"Turn left onto West 36th Street.\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":44.444,\"announcement\":\"Your destination is on the left.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYour destination is on the left.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":88.242,\"primary\":{\"text\":\"Your destination will be on the left\",\"components\":[{\"text\":\"Your destination will be on the left\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":44.444,\"primary\":{\"text\":\"Your destination is on the left\",\"components\":[{\"text\":\"Your destination is on the left\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":26.551,\"intersections\":[{\"turn_weight\":12.5,\"turn_duration\":5.622,\"location\":[-122.680837,45.648134],\"bearings\":[0,181,271],\"entry\":[true,false,true],\"in\":1,\"out\":2,\"geometry_index\":69,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}}]},{\"weight_typical\":0,\"distance\":0.0,\"duration\":0.0,\"duration_typical\":0.0,\"speedLimitUnit\":\"mph\",\"speedLimitSign\":\"mutcd\",\"geometry\":\"cpcavAdf|~hF??\",\"name\":\"West 36th Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.681971,45.648146],\"bearing_before\":271.0,\"bearing_after\":0.0,\"instruction\":\"Your destination is on the left.\",\"type\":\"arrive\",\"modifier\":\"left\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.681971,45.648146],\"bearings\":[91],\"entry\":[true],\"in\":0,\"geometry_index\":70,\"admin_index\":0}]}],\"annotation\":{\"distance\":[45.0,48.6,37.3,88.4,39.0,25.4,33.9,23.3,9.7,7.9,7.8,8.9,23.0,30.8,51.1,10.8,64.6,21.6,48.9,8.9,9.1,8.0,39.0,6.4,21.3,44.4,35.2,8.6,7.7,24.9,74.9,45.6,99.8,84.1,25.4,82.5,25.7,41.1,5.9,18.0,36.7,25.8,34.1,3.5,93.9,9.9,35.4,48.6,28.0,34.6,18.5,59.3,57.4,20.0,20.9,13.1,42.6,40.7,42.7,7.9,10.6,10.5,57.1,6.0,36.7,29.9,24.6,27.7,37.5,88.3],\"duration\":[7.315,8.552,3.937,9.919,4.031,2.832,3.844,2.874,1.249,1.004,2.94,1.053,2.779,3.619,7.363,1.564,9.255,2.75,8.429,1.533,8.608,0.868,4.239,0.69,2.821,5.867,4.718,1.148,2.73,2.261,6.75,3.784,8.007,6.455,1.844,6.246,1.971,3.301,0.547,2.046,4.163,14.405,5.119,0.276,7.069,0.774,2.693,5.059,3.752,3.019,1.649,5.22,4.294,1.619,1.764,1.101,3.795,3.804,4.204,0.71,1.046,1.016,5.149,0.533,3.118,2.579,2.276,2.543,3.128,17.84],\"speed\":[6.1,7.4,9.5,8.9,9.7,9.0,8.9,8.1,7.9,7.9,8.5,8.5,8.3,8.5,7.0,7.0,7.0,7.9,5.8,5.8,9.2,9.2,9.2,9.2,7.6,7.6,7.5,7.5,11.0,11.0,11.1,12.1,12.5,13.1,13.9,13.3,13.1,12.6,11.1,8.9,8.8,2.5,6.7,13.4,13.3,13.1,13.1,9.6,7.5,11.5,11.3,11.3,13.4,12.5,11.9,11.9,11.3,10.7,10.2,11.3,10.3,10.3,11.1,11.7,11.9,11.6,10.9,10.9,12.1,7.2],\"maxspeed\":[{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"}],\"congestion_numeric\":[0,24,9,9,0,0,0,null,null,null,0,0,0,null,6,9,9,14,null,null,9,9,9,9,30,30,30,30,12,12,12,4,3,1,0,0,null,3,null,0,0,null,null,null,0,null,null,null,null,null,null,null,null,null,null,null,0,null,null,null,0,0,null,null,null,null,null,null,0,null]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":\"-122.6716938,45.6344717;-122.681974,45.6479927\",\"alternatives\":true,\"language\":\"en\",\"layers\":\";\",\"continue_straight\":true,\"roundabout_exits\":true,\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion_numeric,maxspeed,closure,speed,duration,distance\",\"voice_instructions\":true,\"banner_instructions\":true,\"voice_units\":\"imperial\",\"enable_refresh\":true},\"voiceLocale\":\"en-US\",\"requestUuid\":\"VA6i2dqAyEvoC4gS25Y14Z6gAE6c2fe_Tpq4H9voaY7FjLb-IUIcYQ\\u003d\\u003d\"}"
        return DirectionsRoute.fromJson(routeJson)
    }
}
