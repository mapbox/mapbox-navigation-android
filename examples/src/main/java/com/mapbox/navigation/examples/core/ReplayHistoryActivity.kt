package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.CustomEventMapper
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset.forName
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.android.synthetic.main.activity_replay_history_layout.*
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * This activity shows how to use replay ride history.
 */
class ReplayHistoryActivity : AppCompatActivity() {

    private var navigationContext: ReplayNavigationContext? = null

    // You choose your loading mechanism. Use Coroutines, ViewModels, RxJava, Threads, etc..
    private var loadNavigationJob: Job? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_replay_history_layout)
        mapView.onCreate(savedInstanceState)

        getNavigationAsync {
            navigationContext = it
            it.onNavigationReady()
        }
    }

    private fun getNavigationAsync(callback: (ReplayNavigationContext) -> Unit) {
        loadNavigationJob = CoroutineScope(Dispatchers.Main).launch {
            // Load map and style on Main dispatchers
            val deferredMapboxWithStyle = async { loadMapWithStyle() }

            // Load and replay history on IO dispatchers
            val deferredEvents = async(Dispatchers.IO) { loadReplayHistory() }
            val replayEvents = deferredEvents.await()
            val mapboxReplay = MapboxReplayer()
                .pushEvents(replayEvents)
            if (!isActive) return@launch

            val locationEngine = ReplayLocationEngine()

            // Await the map and we're ready for navigation
            val mapboxNavigation = createMapboxNavigation(locationEngine)
            val (mapboxMap, style) = deferredMapboxWithStyle.await()
            if (!isActive) return@launch

            initLocationComponent(locationEngine, style, mapboxMap)

            val navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this@ReplayHistoryActivity, true)
            val navigationContext = ReplayNavigationContext(
                locationEngine,
                mapboxMap,
                style,
                mapboxNavigation,
                navigationMapboxMap,
                mapboxReplay
            )
            callback(navigationContext)
        }
    }

    private suspend fun loadMapWithStyle(): Pair<MapboxMap, Style> = suspendCoroutine { cont ->
        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                cont.resume(Pair(mapboxMap, style))
            }
        }
    }

    private suspend fun loadReplayHistory(): List<ReplayEventBase> = suspendCoroutine { cont ->
        val replayHistoryMapper = ReplayHistoryMapper(ReplayCustomEventMapper(), MapboxLogger)
        val rideHistoryExample = loadHistoryJsonFromAssets(this@ReplayHistoryActivity, "replay-history-activity.json")
        val replayEvents = replayHistoryMapper.mapToReplayEvents(rideHistoryExample)
        cont.resume(replayEvents)
    }

    private fun createMapboxNavigation(locationEngine: LocationEngine): MapboxNavigation {
        val accessToken = Utils.getMapboxAccessToken(this)
        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, accessToken)
            .locationEngine(locationEngine)
            .build()

        return MapboxNavigation(mapboxNavigationOptions)
    }

    /**
     * After the map, style, and replay history is all loaded. Connect the view.
     */
    private fun ReplayNavigationContext.onNavigationReady() {
        setupReplayControls()

        navigationMapboxMap.addProgressChangeListener(mapboxNavigation)

        mapboxReplayer.playFirstLocation()
        mapboxMap.addOnMapLongClickListener { latLng ->
            selectMapLocation(latLng)
            true
        }

        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                events.forEach { event ->
                    when (event) {
                        is ReplayEventInitialRoute -> {
                            event.coordinates.lastOrNull()?.let { latLng ->
                                selectMapLocation(latLng)
                            }
                        }
                    }
                }
            }
        })

        playReplay.setOnClickListener {
            mapboxReplayer.play()
        }
    }

    private fun ReplayNavigationContext.setupReplayControls() {
        seekBar.max = 8
        seekBar.progress = 1
        seekBarText.text = getString(R.string.replay_playback_speed_seekbar, seekBar.progress)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mapboxReplayer.playbackSpeed(progress.toDouble())
                seekBarText.text = getString(R.string.replay_playback_speed_seekbar, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
        })
    }

    private fun ReplayNavigationContext.selectMapLocation(latLng: LatLng) {
        mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
            mapboxNavigation.requestRoutes(
                RouteOptions.builder().applyDefaultParams()
                    .accessToken(Utils.getMapboxAccessToken(applicationContext))
                    .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                    .alternatives(true)
                    .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                    .build(),
                routesReqCallback
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun ReplayNavigationContext.startNavigation() {
        if (mapboxNavigation.getRoutes().isNotEmpty()) {
            navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.GPS)
            navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])
        }
        mapboxNavigation.startActiveGuidance()
    }

    @SuppressLint("RestrictedApi")
    private fun initLocationComponent(locationEngine: LocationEngine, loadedMapStyle: Style, mapboxMap: MapboxMap) {
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
        mapboxMap.locationComponent.let { locationComponent ->
            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationEngine(locationEngine)
                    .build()

            locationComponent.activateLocationComponent(locationComponentActivationOptions)
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            MapboxLogger.d(Message("route request success $routes"))
            if (routes.isNotEmpty()) {
                navigationContext?.navigationMapboxMap?.drawRoute(routes[0])
                navigationContext?.startNavigation()
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            MapboxLogger.e(
                Message("route request failure"),
                throwable
            )
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            MapboxLogger.d(Message("route request canceled"))
        }
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
        loadNavigationJob?.cancelChildren()
        navigationContext?.apply {
            mapboxReplayer.finish()
            mapboxNavigation.stopActiveGuidance()
            mapboxNavigation.onDestroy()
        }
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

private data class ReplayNavigationContext(
    val locationEngine: LocationEngine,
    val mapboxMap: MapboxMap,
    val style: Style,
    val mapboxNavigation: MapboxNavigation,
    val navigationMapboxMap: NavigationMapboxMap,
    val mapboxReplayer: MapboxReplayer
)

private fun loadHistoryJsonFromAssets(context: Context, fileName: String): String {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        String(buffer, forName("UTF-8"))
    } catch (e: IOException) {
        MapboxLogger.e(
            Message("Your history file failed to open $fileName"),
            e
        )
        throw e
    }
}

private class ReplayCustomEventMapper : CustomEventMapper {
    override fun map(eventType: String, properties: Map<*, *>): ReplayEventBase? {
        return when (eventType) {
            "start_transit" -> ReplayEventStartTransit(
                eventTimestamp = properties["event_timestamp"] as Double,
                properties = properties["properties"] as Double)
            "initial_route" -> {
                val eventProperties = properties["properties"] as Map<*, *>
                val routeOptions = eventProperties["routeOptions"] as Map<*, *>
                val coordinates = routeOptions["coordinates"] as List<List<Double>>
                val coordinatesLatLng = coordinates.map { LatLng(it[1], it[0]) }
                ReplayEventInitialRoute(
                    eventTimestamp = properties["event_timestamp"] as Double,
                    coordinates = coordinatesLatLng
                )
            }
            else -> null
        }
    }
}

private data class ReplayEventStartTransit(
    override val eventTimestamp: Double,
    val properties: Double
) : ReplayEventBase

private data class ReplayEventInitialRoute(
    override val eventTimestamp: Double,
    val coordinates: List<LatLng>
) : ReplayEventBase
