package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStopType
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollectionType
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.history.ReplayHistorySessionOptions
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.MapboxTripStarter
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityRestStopBinding
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
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.utils.internal.ifNonNull
import java.io.File

class RoadObjectsActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "RoadObjectsActivity"
    }

    private val navigationLocationProvider = NavigationLocationProvider()

    private val binding: LayoutActivityRestStopBinding by lazy {
        LayoutActivityRestStopBinding.inflate(layoutInflater)
    }

    private val distanceFormatterOptions: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(this).build()
    }

    private val formatter: DistanceRemainingFormatter by lazy {
        DistanceRemainingFormatter(distanceFormatterOptions)
    }

    private val mapboxNavigation: MapboxNavigation by lazy {
        MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxSapaAccessToken(this))
                .build()
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private val mapboxTripStarter = MapboxTripStarter.create()
        .enableReplayHistory(
            options = ReplayHistorySessionOptions.Builder()
                .filePath(getFirstHistoryFile())
                .build()
        )

    fun getFirstHistoryFile(): String? {
        return mapboxNavigation.historyRecorder.fileDirectory()?.let { File(it) }
            ?.listFiles()?.firstOrNull()?.absolutePath
    }

    private val mapCamera: CameraAnimationsPlugin by lazy {
        binding.mapView.camera
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(RouteLineResources.Builder().build())
            .withRouteLineBelowLayerId("road-label-navigation")
            .displayRestrictedRoadSections(true)
            .withVanishingRouteLineEnabled(true)
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

    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }

    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(routeArrowOptions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initNavigation()
        initReplayer()
        initStyle()
        initListeners()
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onDestroy() {
        super.onDestroy()
        mapboxTripStarter.onDetached(mapboxNavigation)
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxNavigation.onDestroy()
    }

    private fun initStyle() {
        binding.mapView.getMapboxMap().loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE
        ) { style ->

            val route = getRoute()
            routeLineApi.setRoutes(listOf(RouteLine(route, null))) {
                routeLineView.renderRouteDrawData(style, it)
            }
        }
    }

    private fun initNavigation() {
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun initReplayer() {
        mapboxTripStarter.onAttached(mapboxNavigation)
        val routeOrigin = Utils.getRouteOriginPoint(getRoute())
        mapboxNavigation.mapboxReplayer.pushEvents(
            listOf(ReplayRouteMapper.mapToUpdateLocation(0.0, routeOrigin))
        )
    }

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            updateCamera(locationMatcherResult.enhancedLocation)
        }
    }

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .zoom(15.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        binding.startNavigation.setOnClickListener {
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            binding.startNavigation.visibility = View.GONE
            mapboxNavigation.setRoutes(listOf(getRoute()))
        }
    }

    private val routesObserver = RoutesObserver {
        val builder = SpannableStringBuilder("The route has\n\n")
        val navigationRoute = it.navigationRoutes.first()
        val allBridges = navigationRoute.upcomingRoadObjects.filter { upcoming ->
            upcoming.roadObject.objectType == RoadObjectType.BRIDGE
        }
        val allBorderCrossings = navigationRoute.upcomingRoadObjects.filter { upcoming ->
            upcoming.roadObject.objectType == RoadObjectType.COUNTRY_BORDER_CROSSING
        }
        val allIncidents = navigationRoute.upcomingRoadObjects.filter { upcoming ->
            upcoming.roadObject.objectType == RoadObjectType.INCIDENT
        }
        val allRailwayCrossings = navigationRoute.upcomingRoadObjects.filter { upcoming ->
            upcoming.roadObject.objectType == RoadObjectType.RAILWAY_CROSSING
        }
        val allRestrictedAreas = navigationRoute.upcomingRoadObjects.filter { upcoming ->
            upcoming.roadObject.objectType == RoadObjectType.RESTRICTED_AREA
        }
        val allRestStops = navigationRoute.upcomingRoadObjects.filter { upcoming ->
            upcoming.roadObject.objectType == RoadObjectType.REST_STOP
        }
        val allTollCollections = navigationRoute.upcomingRoadObjects.filter { upcoming ->
            upcoming.roadObject.objectType == RoadObjectType.TOLL_COLLECTION
        }
        val allTunnels = navigationRoute.upcomingRoadObjects.filter { upcoming ->
            upcoming.roadObject.objectType == RoadObjectType.TUNNEL
        }
        builder.append("${allBridges.size} bridges\n")
        builder.append("${allBorderCrossings.size} border crossings\n")
        builder.append("${allIncidents.size} incidents\n")
        builder.append("${allRailwayCrossings.size} railway crossings\n")
        builder.append("${allRestrictedAreas.size} restricted areas\n")
        builder.append("${allRestStops.size} rest stops\n")
        builder.append("${allTollCollections.size} toll collections\n")
        builder.append("${allTunnels.size} tunnels\n")
        binding.allRoadObjects.text = builder
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

        val upcomingBridge = routeProgress.getFirstUpcomingRoadObject(RoadObjectType.BRIDGE)
        val upcomingBorderCrossing = routeProgress.getFirstUpcomingRoadObject(
            RoadObjectType.COUNTRY_BORDER_CROSSING
        )
        val upcomingIncident = routeProgress.getFirstUpcomingRoadObject(RoadObjectType.INCIDENT)
        val upcomingRailwayCrossing = routeProgress.getFirstUpcomingRoadObject(
            RoadObjectType.RAILWAY_CROSSING
        )
        val upcomingRestrictedArea = routeProgress.getFirstUpcomingRoadObject(
            RoadObjectType.RESTRICTED_AREA
        )
        val upcomingRestStop = routeProgress.getFirstUpcomingRoadObject(RoadObjectType.REST_STOP)
        val upcomingTollCollection = routeProgress.getFirstUpcomingRoadObject(
            RoadObjectType.TOLL_COLLECTION
        )
        val upcomingTunnel = routeProgress.getFirstUpcomingRoadObject(RoadObjectType.TUNNEL)
        val upcomingInterchange = routeProgress.getFirstUpcomingRoadObject(RoadObjectType.IC)
        val upcomingJunction = routeProgress.getFirstUpcomingRoadObject(RoadObjectType.JCT)

        binding.bridge.text = ifNonNull(upcomingBridge?.distanceToStart) { distance ->
            getRoadObject(R.string.upcoming_bridge, distance)
        } ?: getString(R.string.no_upcoming_bridge)

        binding.border.text = ifNonNull(upcomingBorderCrossing?.distanceToStart) { distance ->
            getRoadObject(R.string.upcoming_border_crossing, distance)
        } ?: getString(R.string.no_upcoming_border_crossing)

        binding.incident.text = ifNonNull(upcomingIncident?.distanceToStart) { distance ->
            getRoadObject(R.string.upcoming_incident, distance)
        } ?: getString(R.string.no_upcoming_incident)

        binding.railway.text = ifNonNull(upcomingRailwayCrossing?.distanceToStart) { distance ->
            getRoadObject(R.string.upcoming_railway_crossing, distance)
        } ?: getString(R.string.no_upcoming_railway_crossing)

        binding.restricted.text = ifNonNull(upcomingRestrictedArea?.distanceToStart) { distance ->
            getRoadObject(R.string.upcoming_restricted_area, distance)
        } ?: getString(R.string.no_upcoming_restricted_area)

        binding.rest.text = upcomingRestStop?.let { restStop ->
            restStop.distanceToStart?.let { distance ->
                when ((restStop.roadObject as RestStop).restStopType) {
                    RestStopType.REST_AREA ->
                        getRoadObject(R.string.upcoming_rest_area, distance)
                    RestStopType.SERVICE_AREA ->
                        getRoadObject(R.string.upcoming_service_area, distance)
                    else -> getString(R.string.no_upcoming_rest_stop)
                }
            } ?: getString(R.string.no_upcoming_rest_stop)
        } ?: getString(R.string.no_upcoming_rest_stop)

        binding.toll.text = upcomingTollCollection?.let { tollCollection ->
            tollCollection.distanceToStart?.let { distance ->
                when ((tollCollection.roadObject as TollCollection).tollCollectionType) {
                    TollCollectionType.TOLL_BOOTH -> {
                        getRoadObject(R.string.upcoming_toll_booth, distance)
                    }
                    TollCollectionType.TOLL_GANTRY -> {
                        getRoadObject(R.string.upcoming_toll_gantry, distance)
                    }
                    else -> getString(R.string.no_upcoming_toll_collection)
                }
            } ?: getString(R.string.no_upcoming_toll_collection)
        } ?: getString(R.string.no_upcoming_toll_collection)

        binding.tunnel.text = ifNonNull(upcomingTunnel?.distanceToStart) { distance ->
            getRoadObject(R.string.upcoming_tunnel, distance)
        } ?: getString(R.string.no_upcoming_tunnel)

        binding.ic.text = ifNonNull(upcomingInterchange?.distanceToStart) { distance ->
            getRoadObject(R.string.upcoming_interchange, distance)
        } ?: getString(R.string.no_upcoming_interchange)
        binding.jct.text = ifNonNull(upcomingJunction?.distanceToStart) { distance ->
            getRoadObject(R.string.upcoming_junction, distance)
        } ?: getString(R.string.no_upcoming_junction)
    }

    private fun getRoadObject(
        @StringRes stringResource: Int,
        distance: Double
    ): String {
        return getString(stringResource, formatter.format(distance))
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsString = Utils.readRawFileText(this, R.raw.route_with_sapa)
        return DirectionsRoute.fromJson(routeAsString)
    }

    private fun RouteProgress.getFirstUpcomingRoadObject(
        @RoadObjectType.Type type: Int
    ): UpcomingRoadObject? {
        return this.upcomingRoadObjects.firstOrNull {
            if (it.roadObject.objectType == type) {
                val distanceToStart = it.distanceToStart
                distanceToStart != null && distanceToStart > 0
            } else {
                false
            }
        }
    }
}
