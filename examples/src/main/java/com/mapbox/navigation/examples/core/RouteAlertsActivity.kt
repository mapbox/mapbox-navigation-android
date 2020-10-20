package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.alert.CountryBorderCrossingAlert
import com.mapbox.navigation.base.trip.model.alert.IncidentAlert
import com.mapbox.navigation.base.trip.model.alert.RestStopAlert
import com.mapbox.navigation.base.trip.model.alert.RestStopType
import com.mapbox.navigation.base.trip.model.alert.RestrictedAreaAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlertType
import com.mapbox.navigation.base.trip.model.alert.TunnelEntranceAlert
import com.mapbox.navigation.base.trip.model.alert.toLineString
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.RouteAlertsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.routealert.MapboxRouteAlertsDisplayer
import com.mapbox.navigation.ui.routealert.MapboxRouteAlertsDisplayerOptions
import kotlinx.android.synthetic.main.activity_replay_route_layout.mapView
import kotlinx.android.synthetic.main.activity_route_alerts.distanceRemainingText
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class RouteAlertsActivity : AppCompatActivity() {

    private val mapboxRouteAlertsDisplayer: MapboxRouteAlertsDisplayer by lazy {
        MapboxRouteAlertsDisplayer(
            MapboxRouteAlertsDisplayerOptions.Builder(this)
                .showToll(true)
                .build()
        )
    }

    private val directionsRoute: DirectionsRoute by lazy {
        val directionsResponseJson = resources.openRawResource(
            R.raw.mock_response_all_route_alerts_polyline6
        )
            .bufferedReader()
            .use { it.readText() }
        DirectionsResponse.fromJson(directionsResponseJson).routes()[0]
    }
    private val routeLineString: LineString by lazy {
        LineString.fromPolyline(directionsRoute.geometry()!!, Constants.PRECISION_6)
    }

    private val blueCircleImageId = "circle_blue_image"
    private val greenCircleImageId = "circle_green_image"
    private val yellowCircleImageId = "circle_yellow_image"
    private val blackCircleImageId = "circle_black_image"

    // tunnels
    private val tunnelSource = GeoJsonSource("tunnel_source")
    private val tunnelLayer = LineLayer("tunnel_layer", "tunnel_source")
        .withProperties(
            *generateLineLayerProperties(Color.DKGRAY)
        )

    private val tunnelNamesTextPropertyId = "tunnel_names_text_property"
    private val tunnelNamesSource = GeoJsonSource("tunnel_names_source")
    private val tunnelNamesLayer = SymbolLayer(
        "tunnel_names_layer",
        "tunnel_names_source"
    ).withProperties(
        *generateTunnelNamesLayerProperties(yellowCircleImageId, tunnelNamesTextPropertyId)
    )

    // country border crossings
    private val countryBorderCrossingsTextPropertyId = "country_border_crossings_text_property"
    private val countryBorderCrossingsSource = GeoJsonSource("country_border_crossings_source")
    private val countryBorderCrossingsLayer = SymbolLayer(
        "country_border_crossings_layer",
        "country_border_crossings_source"
    ).withProperties(
        *generateSymbolLayerProperties(blueCircleImageId, countryBorderCrossingsTextPropertyId)
    )

    // rest stop
    private val restStopTextPropertyId = "rest_stops_text_property"
    private val restStopSource = GeoJsonSource("rest_stops_source")
    private val restStopLayer = SymbolLayer(
        "rest_stops_layer",
        "rest_stops_source"
    ).withProperties(
        *generateSymbolLayerProperties(greenCircleImageId, restStopTextPropertyId)
    )

    // tunnels
    private val restrictedAreasSource = GeoJsonSource("restricted_areas_source")
    private val restrictedAreasLayer = LineLayer(
        "restricted_areas_layer",
        "restricted_areas_source"
    ).withProperties(
        *generateLineLayerProperties(Color.RED)
    )

    // incidents
    private val incidentSource = GeoJsonSource("incident_source")
    private val incidentLayer = LineLayer(
        "incident_layer",
        "incident_source"
    ).withProperties(
        *generateLineLayerProperties(Color.rgb(255, 20, 147)) // deep pink
    )

    private val incidentNamesTextPropertyId = "incident_names_text_property"
    private val incidentNamesSource = GeoJsonSource("incident_names_source")
    private val incidentNamesLayer = SymbolLayer(
        "incident_names_layer",
        "incident_names_source"
    ).withProperties(
        *generateTunnelNamesLayerProperties(blackCircleImageId, incidentNamesTextPropertyId)
    )

    private val mapboxReplayer: MapboxReplayer by lazy {
        MapboxReplayer().apply {
            val replayEvents = ReplayRouteMapper().mapGeometry(directionsRoute.geometry()!!)
            pushEvents(replayEvents)
            seekTo(replayEvents.first())
        }
    }

    private var navigationMapboxMap: NavigationMapboxMap? = null
    private val mapboxNavigation: MapboxNavigation by lazy {
        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()

        MapboxNavigation(mapboxNavigationOptions)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_alerts)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { mapboxMap ->
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(16.0))
            mapboxMap.moveCamera(CameraUpdateFactory.tiltTo(45.0))
            mapboxMap.setStyle(getString(R.string.mapbox_navigation_guidance_day)) { style ->
                navigationMapboxMap = NavigationMapboxMap.Builder(mapView, mapboxMap, this).build()
                mapboxNavigation.setRoutes(listOf(directionsRoute))
                navigationMapboxMap?.apply {
                    updateLocationLayerRenderMode(RenderMode.GPS)
                    updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                    addProgressChangeListener(mapboxNavigation)
                    drawRoute(directionsRoute)
                    startCamera(directionsRoute)
                    mapboxReplayer.play()
                }

                mapboxRouteAlertsDisplayer.onStyleLoaded(style)

                style.addImage(
                    blueCircleImageId,
                    ContextCompat.getDrawable(this, R.drawable.ic_circle_blue)!!
                )
                style.addImage(
                    greenCircleImageId,
                    ContextCompat.getDrawable(this, R.drawable.ic_circle_green)!!
                )
                style.addImage(
                    yellowCircleImageId,
                    ContextCompat.getDrawable(this, R.drawable.ic_circle_yellow)!!
                )
                style.addImage(
                    blackCircleImageId,
                    ContextCompat.getDrawable(this, R.drawable.ic_circle_black)!!
                )
                style.addSource(tunnelSource)
                style.addLayerBelow(tunnelLayer, RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
                style.addSource(tunnelNamesSource)
                style.addLayerBelow(
                    tunnelNamesLayer,
                    RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID
                )
                style.addSource(countryBorderCrossingsSource)
                style.addLayerBelow(
                    countryBorderCrossingsLayer,
                    RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID
                )
                style.addSource(restStopSource)
                style.addLayerBelow(restStopLayer, RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
                style.addSource(restrictedAreasSource)
                style.addLayerBelow(
                    restrictedAreasLayer,
                    RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID
                )
                style.addSource(incidentSource)
                style.addLayerBelow(incidentLayer, RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
                style.addSource(incidentNamesSource)
                style.addLayerBelow(
                    incidentNamesLayer,
                    RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID
                )
            }
        }
        mapboxNavigation.toggleHistory(true)
        mapboxNavigation.startTripSession()
        mapboxNavigation.registerRouteProgressObserver(
            object : RouteProgressObserver {
                @SuppressLint("SetTextI18n")
                override fun onRouteProgressChanged(routeProgress: RouteProgress) {
                    // in this listener we're constantly updating
                    // the distance to the start of the upcoming tunnel
                    val upcomingTunnel = routeProgress
                        .upcomingRouteAlerts
                        .firstOrNull { it.routeAlert.alertType == RouteAlertType.TunnelEntrance }
                    if (upcomingTunnel != null) {
                        val distanceToStart = upcomingTunnel.distanceToStart.roundToInt()
                        if (distanceToStart > 0) {
                            distanceRemainingText.text =
                                """
                                    |Distance to the nearest tunnel:
                                    |$distanceToStart meters
                                """.trimMargin()
                        } else {
                            // if the distance to start is negative,
                            // it means that we're inside of the tunnel
                            distanceRemainingText.text =
                                """
                                    |You're in the tunnel.
                                    |You've traveled ${distanceToStart.absoluteValue} meters since entry.
                                """.trimMargin()
                        }
                    } else {
                        distanceRemainingText.text = ""
                    }
                }
            }
        )

        mapboxNavigation.registerRouteAlertsObserver(
            object : RouteAlertsObserver {
                override fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
                    mapboxRouteAlertsDisplayer.onNewRouteAlerts(routeAlerts)

                    // in this part of the example we're listening for the full list of alerts
                    // whenever a new route is set and marking all of the tunnels on the map
                    val tunnelFeatures = mutableListOf<Feature>()
                    val tunnelNamesFeatures = mutableListOf<Feature>()
                    val countryBorderCrossingsFeatures = mutableListOf<Feature>()
                    val restStopsFeatures = mutableListOf<Feature>()
                    val restrictedAreasFeatures = mutableListOf<Feature>()
                    val incidentsFeatures = mutableListOf<Feature>()
                    val incidentsNamesFeatures = mutableListOf<Feature>()

                    // when received, we're filling up features for all alert types
                    routeAlerts.forEach { routeAlert ->
                        when (routeAlert) {
                            is TunnelEntranceAlert -> {
                                val alertGeometry = routeAlert.alertGeometry
                                if (alertGeometry != null) {
                                    val tunnelLineString =
                                        alertGeometry.toLineString(routeLineString)
                                    tunnelFeatures.add(Feature.fromGeometry(tunnelLineString))
                                    routeAlert.info?.name.let {
                                        val feature = Feature.fromGeometry(routeAlert.coordinate)
                                        feature.addStringProperty(
                                            tunnelNamesTextPropertyId,
                                            it
                                        )
                                        tunnelNamesFeatures.add(feature)
                                    }
                                } else {
                                    throw IllegalArgumentException("missing tunnel geometry")
                                }
                            }
                            is CountryBorderCrossingAlert -> {
                                val from = routeAlert.from!!.codeAlpha3
                                val to = routeAlert.to!!.codeAlpha3
                                val feature = Feature.fromGeometry(routeAlert.coordinate)
                                feature.addStringProperty(
                                    countryBorderCrossingsTextPropertyId,
                                    "$from -> $to"
                                )
                                countryBorderCrossingsFeatures.add(feature)
                            }
                            is RestStopAlert -> {
                                val typeString = when (routeAlert.restStopType) {
                                    RestStopType.RestArea -> {
                                        "rest area"
                                    }
                                    RestStopType.Unknown -> {
                                        "unknown"
                                    }
                                    else -> {
                                        throw IllegalArgumentException(
                                            "unknown rest stop type"
                                        )
                                    }
                                }
                                val feature = Feature.fromGeometry(routeAlert.coordinate)
                                feature.addStringProperty(
                                    restStopTextPropertyId,
                                    typeString
                                )
                                restStopsFeatures.add(feature)
                            }
                            is RestrictedAreaAlert -> {
                                val alertGeometry = routeAlert.alertGeometry
                                if (alertGeometry != null) {
                                    val restrictedAreaLineString =
                                        alertGeometry.toLineString(routeLineString)
                                    restrictedAreasFeatures.add(
                                        Feature.fromGeometry(restrictedAreaLineString)
                                    )
                                } else {
                                    throw IllegalArgumentException(
                                        "missing restricted area geometry"
                                    )
                                }
                            }
                            is IncidentAlert -> {
                                val alertGeometry = routeAlert.alertGeometry
                                if (alertGeometry != null) {
                                    val incidentAreaLineString =
                                        alertGeometry.toLineString(routeLineString)
                                    incidentsFeatures.add(
                                        Feature.fromGeometry(incidentAreaLineString)
                                    )
                                }
                                routeAlert.info?.subType?.let { incidentType ->
                                    val feature = Feature.fromGeometry(routeAlert.coordinate)
                                    feature.addStringProperty(
                                        incidentNamesTextPropertyId,
                                        incidentType
                                    )
                                    incidentsNamesFeatures.add(feature)
                                }
                            }
                        }
                    }

                    // update all sources
                    tunnelSource.setGeoJson(FeatureCollection.fromFeatures(tunnelFeatures))
                    tunnelNamesSource.setGeoJson(
                        FeatureCollection.fromFeatures(tunnelNamesFeatures)
                    )
                    countryBorderCrossingsSource.setGeoJson(
                        FeatureCollection.fromFeatures(countryBorderCrossingsFeatures)
                    )
                    restStopSource.setGeoJson(
                        FeatureCollection.fromFeatures(restStopsFeatures)
                    )
                    restrictedAreasSource.setGeoJson(
                        FeatureCollection.fromFeatures(restrictedAreasFeatures)
                    )
                    incidentSource.setGeoJson(
                        FeatureCollection.fromFeatures(incidentsFeatures)
                    )
                    incidentNamesSource.setGeoJson(
                        FeatureCollection.fromFeatures(incidentsNamesFeatures)
                    )
                }
            }
        )
    }

    private fun generateSymbolLayerProperties(
        imageId: String,
        textId: String
    ): Array<PropertyValue<out Any>> = arrayOf(
        PropertyFactory.iconImage(imageId),
        PropertyFactory.iconSize(3f),
        PropertyFactory.textField(
            Expression.get(Expression.literal(textId))
        ),
        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM_LEFT),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true)
    )

    private fun generateTunnelNamesLayerProperties(
        imageId: String,
        textId: String
    ): Array<PropertyValue<out Any>> = arrayOf(
        PropertyFactory.iconImage(imageId),
        PropertyFactory.iconSize(3f),
        PropertyFactory.textField(
            Expression.get(Expression.literal(textId))
        ),
        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM_RIGHT),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true),
        PropertyFactory.textAllowOverlap(true),
        PropertyFactory.textIgnorePlacement(true)
    )

    private fun generateLineLayerProperties(
        @ColorInt color: Int
    ): Array<PropertyValue<out Any>> =
        arrayOf(
            PropertyFactory.lineColor(color),
            PropertyFactory.lineWidth(10f)
        )

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapboxReplayer.finish()
        mapboxNavigation.stopTripSession()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
