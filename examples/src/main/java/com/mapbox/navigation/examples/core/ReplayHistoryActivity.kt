package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplaySetNavigationRoute
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.ActivityReplayHistoryLayoutBinding
import com.mapbox.navigation.examples.core.replay.HistoryFileLoader
import com.mapbox.navigation.examples.core.replay.HistoryFilesActivity
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Collections

private const val DEFAULT_INITIAL_ZOOM = 15.0

class ReplayHistoryActivity : AppCompatActivity() {

    private var loadNavigationJob: Job? = null
    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var historyFileLoader: HistoryFileLoader
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var mapboxReplayer: MapboxReplayer
    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var binding: ActivityReplayHistoryLayoutBinding
    private var isLocationInitialized = false
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            20.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    private val initialCameraOptions: CameraOptions? = CameraOptions.Builder()
        .zoom(DEFAULT_INITIAL_ZOOM)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReplayHistoryLayoutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initNavigation()
        handleHistoryFileSelected()
        initMapStyle()

        findViewById<Button>(R.id.selectHistoryButton).setOnClickListener {
            val activityIntent = Intent(this, HistoryFilesActivity::class.java)
                .putExtra(
                    HistoryFilesActivity.EXTRA_HISTORY_FILE_DIRECTORY,
                    mapboxNavigation.historyRecorder.fileDirectory()
                )
            startActivityForResult(activityIntent, HistoryFilesActivity.REQUEST_CODE)
        }
        setupReplayControls()

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
    }

    override fun onStart() {
        super.onStart()

        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
        if (::locationComponent.isInitialized) {
            locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initMapStyle() {
        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap()
        )
        val mapboxMap = binding.mapView.getMapboxMap()
        navigationCamera = NavigationCamera(
            mapboxMap,
            binding.mapView.camera,
            viewportDataSource
        )
        initialCameraOptions?.let { mapboxMap.setCamera(it) }
        mapboxMap.loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE,
            {
                locationComponent = binding.mapView.location.apply {
                    this.locationPuck = LocationPuck2D(
                        bearingImage = ContextCompat.getDrawable(
                            this@ReplayHistoryActivity,
                            R.drawable.mapbox_navigation_puck_icon
                        )
                    )
                    setLocationProvider(navigationLocationProvider)
                    enabled = true
                }
                locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)
                viewportDataSource.evaluate()
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    // intentionally blank
                }
            }
        )
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()
            if (!isLocationInitialized) {
                isLocationInitialized = true
                val instantTransition = NavigationCameraTransitionOptions.Builder()
                    .maxDuration(0)
                    .build()
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = instantTransition,
                )
            }

            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
        }
    }

    /** Rendering the set route event **/

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(
                RouteLineResources.Builder()
                    .routeLineColorResources(
                        RouteLineColorResources.Builder().build()
                    )
                    .build()
            )
            .withRouteLineBelowLayerId("road-label-navigation")
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

    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(
            RouteArrowOptions.Builder(this)
                .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
                .build()
        )
    }

    private val routesObserver: RoutesObserver = RoutesObserver { result ->
        if (result.routes.isEmpty()) {
            viewportDataSource.clearRouteData()
        } else {
            viewportDataSource.onRouteChanged(result.routes.first())
        }
        viewportDataSource.evaluate()

        val routeLines = result.navigationRoutes.map { NavigationRouteLine(it, null) }
        routeLineApi.setNavigationRouteLines(routeLines) { value ->
            binding.mapView.getMapboxMap().getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

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

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        binding.mapView.getMapboxMap().getStyle()?.apply {
            // Render the result to update the map.
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        historyFileLoader = HistoryFileLoader()
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .build()
        )
        startReplayTripSession()
    }

    /**
     * This is showcasing a new way to replay rides at runtime.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun startReplayTripSession() {
        mapboxReplayer = mapboxNavigation.mapboxReplayer
        mapboxNavigation.startReplayTripSession()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == HistoryFilesActivity.REQUEST_CODE) {
            handleHistoryFileSelected()
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleHistoryFileSelected() {
        loadNavigationJob = CoroutineScope(Dispatchers.Main).launch {
            mapboxReplayer.clearEvents()
            val eventStream = historyFileLoader
                .loadReplayHistory(this@ReplayHistoryActivity)
            mapboxReplayer.attachStream(eventStream)
            binding.playReplay.visibility = View.VISIBLE
            mapboxNavigation.resetTripSession()
            mapboxNavigation.setNavigationRoutes(emptyList())
            isLocationInitialized = false
            mapboxReplayer.playFirstLocation()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateReplayStatus(playbackEvents: List<ReplayEventBase>) {
        playbackEvents.lastOrNull()?.eventTimestamp?.let {
            val currentSecond = mapboxReplayer.eventSeconds(it).toInt()
            val durationSecond = mapboxReplayer.durationSeconds().toInt()
            binding.playerStatus.text = "$currentSecond:$durationSecond"
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupReplayControls() {
        binding.seekBar.max = 8
        binding.seekBar.progress = 1
        binding.seekBarText.text = getString(
            R.string.replay_playback_speed_seekbar,
            binding.seekBar.progress
        )
        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    mapboxReplayer.playbackSpeed(progress.toDouble())
                    binding.seekBarText.text = getString(
                        R.string.replay_playback_speed_seekbar,
                        progress
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )

        binding.playReplay.setOnClickListener {
            mapboxReplayer.play()
            binding.playReplay.visibility = View.GONE
            navigationCamera.requestNavigationCameraToFollowing()
        }

        mapboxReplayer.registerObserver { events ->
            updateReplayStatus(events)
            events.forEach {
                when (it) {
                    is ReplaySetNavigationRoute -> setRoute(it)
                }
            }
        }
    }

    private fun setRoute(replaySetRoute: ReplaySetNavigationRoute) {
        replaySetRoute.route?.let { directionRoute ->
            mapboxNavigation.setNavigationRoutes(Collections.singletonList(directionRoute))
        }
    }
}
