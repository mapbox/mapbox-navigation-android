package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.CustomEventMapper
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.core.replay.history.ReplaySetRoute
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.history.HistoryFilesActivity
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_replay_history_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.nio.charset.Charset.forName
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

        selectHistoryButton.setOnClickListener {
            val activityIntent = Intent(this, HistoryFilesActivity::class.java)
            startActivityForResult(activityIntent, HistoryFilesActivity.REQUEST_CODE)
        }

        getNavigationAsync {
            navigationContext = it
            it.onNavigationReady()

            it.mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            it.navigationMapboxMap
                .updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            it.locationEngine.getLastLocation(FirstLocationCallback(it))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == HistoryFilesActivity.REQUEST_CODE) {
            navigationContext?.handleHistoryFileSelected()
        }
    }

    @SuppressLint("MissingPermission")
    private fun ReplayNavigationContext.handleHistoryFileSelected() {
        loadNavigationJob = CoroutineScope(Dispatchers.Main).launch {
            val events = loadReplayHistory()
            mapboxReplayer.clearEvents()
            mapboxReplayer.pushEvents(events)
            mapboxNavigation.resetTripSession()
            mapboxReplayer.playFirstLocation()
            if (mapboxNavigation.getTripSessionState() == TripSessionState.STOPPED) {
                val navigationContext = this@handleHistoryFileSelected
                locationEngine.getLastLocation(FirstLocationCallback(navigationContext))
            }
        }
    }

    private fun getNavigationAsync(callback: (ReplayNavigationContext) -> Unit) {
        loadNavigationJob = CoroutineScope(Dispatchers.Main).launch {
            // Load map and style on Main dispatchers
            val deferredMapboxWithStyle = async { loadMapWithStyle() }

            val replayEvents = loadReplayHistory()
            val mapboxReplay = MapboxReplayer()
                .pushEvents(replayEvents)
            if (!isActive) return@launch

            val locationEngine = ReplayLocationEngine(mapboxReplay)

            // Await the map and we're ready for navigation
            val mapboxNavigation = createMapboxNavigation(locationEngine)
            val (mapboxMap, style) = deferredMapboxWithStyle.await()
            if (!isActive) return@launch

            val navigationMapboxMap =
                NavigationMapboxMap(mapView, mapboxMap, this@ReplayHistoryActivity, true)
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

    private suspend fun loadReplayHistory(): List<ReplayEventBase> = withContext(Dispatchers.IO) {
        HistoryFilesActivity.selectedHistory?.let {
            val replayHistoryMapper = ReplayHistoryMapper(ReplayCustomEventMapper(), MapboxLogger)
            replayHistoryMapper.mapToReplayEvents(it)
        } ?: loadDefaultReplayHistory()
    }

    private suspend fun loadDefaultReplayHistory(): List<ReplayEventBase> =
        withContext(Dispatchers.IO) {
            val replayHistoryMapper = ReplayHistoryMapper(ReplayCustomEventMapper(), MapboxLogger)
            val rideHistoryExample =
                loadHistoryJsonFromAssets(
                    this@ReplayHistoryActivity,
                    "replay-history-activity.json"
                )
            replayHistoryMapper.mapToReplayEvents(rideHistoryExample)
        }

    private fun createMapboxNavigation(locationEngine: LocationEngine): MapboxNavigation {
        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
            .locationEngine(locationEngine)
            .build()
        return MapboxNavigation(mapboxNavigationOptions)
    }

    /**
     * After the map, style, and replay history is all loaded. Connect the view.
     */
    @SuppressLint("MissingPermission")
    private fun ReplayNavigationContext.onNavigationReady() {
        setupReplayControls()

        navigationMapboxMap.addProgressChangeListener(mapboxNavigation)

        mapboxReplayer.playFirstLocation()
        mapboxMap.addOnMapLongClickListener { latLng ->
            selectMapLocation(latLng)
            true
        }

        mapboxReplayer.registerObserver(
            object : ReplayEventsObserver {
                override fun replayEvents(events: List<ReplayEventBase>) {
                    events.forEach { event ->
                        when (event) {
                            is ReplaySetRoute -> {
                                event.route?.let { directionsRoute ->
                                    val routes = Collections.singletonList(directionsRoute)
                                    mapboxNavigation.setRoutes(routes)
                                    navigationContext?.startNavigation()
                                }
                            }
                        }
                    }
                    updateReplayStatus(events)
                }
            }
        )

        mapboxNavigation.attachFasterRouteObserver(
            object : FasterRouteObserver {
                override fun onFasterRoute(
                    currentRoute: DirectionsRoute,
                    alternatives: List<DirectionsRoute>,
                    isAlternativeFaster: Boolean
                ) {
                    navigationContext?.navigationMapboxMap?.drawRoutes(alternatives)
                    navigationContext?.mapboxNavigation?.setRoutes(alternatives)
                }
            }
        )

        playReplay.setOnClickListener {
            mapboxReplayer.play()
            mapboxNavigation.startTripSession()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun ReplayNavigationContext.updateReplayStatus(playbackEvents: List<ReplayEventBase>) {
        playbackEvents.lastOrNull()?.eventTimestamp?.let {
            val currentSecond = mapboxReplayer.eventSeconds(it).toInt()
            val durationSecond = mapboxReplayer.durationSeconds().toInt()
            playerStatus.text = "$currentSecond:$durationSecond"
        }
    }

    private fun ReplayNavigationContext.setupReplayControls() {
        seekBar.max = 8
        seekBar.progress = 1
        seekBarText.text = getString(R.string.replay_playback_speed_seekbar, seekBar.progress)
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    mapboxReplayer.playbackSpeed(progress.toDouble())
                    seekBarText.text = getString(R.string.replay_playback_speed_seekbar, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )
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
            navigationMapboxMap
                .updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])
        }
        mapboxNavigation.startTripSession()
    }

    private class FirstLocationCallback(navigationContext: ReplayNavigationContext) :
        LocationEngineCallback<LocationEngineResult> {

        private val navigationContextRef = WeakReference(navigationContext)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                navigationContextRef
                    .get()
                    ?.navigationMapboxMap
                    ?.updateLocation(result.lastLocation)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            MapboxLogger.d(Message("route request success $routes"))
            if (routes.isNotEmpty()) {
                navigationContext?.navigationMapboxMap?.drawRoutes(routes)
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
            mapboxNavigation.stopTripSession()
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
                properties = properties["properties"] as Double
            )
            else -> null
        }
    }
}

private data class ReplayEventStartTransit(
    override val eventTimestamp: Double,
    val properties: Double
) : ReplayEventBase
