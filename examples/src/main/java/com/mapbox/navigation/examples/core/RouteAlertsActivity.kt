package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
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
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_replay_route_layout.mapView
import kotlinx.android.synthetic.main.activity_route_alerts.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class RouteAlertsActivity : AppCompatActivity() {
    private val directionsRoute: DirectionsRoute by lazy {
        val directionsResponseJson = resources.openRawResource(
            R.raw.route_response_tunnel_driving
            // here's an alternative route with all fake data,
            // we can use it in the future to test the rest of route alerts
            // R.raw.mock_response_all_passive_maneuvers_polyline6
        )
            .bufferedReader()
            .use { it.readText() }
        DirectionsResponse.fromJson(directionsResponseJson).routes()[0]
    }
    private val routeLineString: LineString by lazy {
        LineString.fromPolyline(directionsRoute.geometry()!!, Constants.PRECISION_6)
    }
    private val tunnelFeatures = mutableListOf<Feature>()
    private val tunnelSource = GeoJsonSource("tunnel_source")
    private val tunnelLayer = LineLayer("tunnel_layer", "tunnel_source")
        .withProperties(
            PropertyFactory.lineColor(Color.DKGRAY),
            PropertyFactory.lineWidth(15f)
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
                style.addSource(tunnelSource)
                style.addLayer(tunnelLayer)
                navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this)
                mapboxNavigation.setRoutes(listOf(directionsRoute))
                navigationMapboxMap?.apply {
                    updateLocationLayerRenderMode(RenderMode.GPS)
                    updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                    addProgressChangeListener(mapboxNavigation)
                    drawRoute(directionsRoute)
                    startCamera(directionsRoute)
                    mapboxReplayer.play()
                }
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
                        .firstOrNull { it.routeAlert.type == RouteAlertType.TunnelEntrance }
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
                override fun onNewRouteAlerts(routeAlerts: List<RouteAlert<*>>?) {
                    // in this part of the example we're listening for the full list of alerts
                    // whenever a new route is set and marking all of the tunnels on the map
                    tunnelFeatures.clear()
                    routeAlerts?.forEach { routeAlert ->
                        when (routeAlert) {
                            is TunnelEntranceAlert -> {
                                val alertGeometry = routeAlert.alertGeometry
                                if (alertGeometry != null) {
                                    val tunnelLineString =
                                        alertGeometry.toLineString(routeLineString)
                                    tunnelFeatures.add(Feature.fromGeometry(tunnelLineString))
                                    tunnelSource.setGeoJson(
                                        FeatureCollection.fromFeatures(tunnelFeatures)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

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
