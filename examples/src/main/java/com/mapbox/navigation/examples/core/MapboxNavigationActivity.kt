package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityNavigationBinding
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MapboxNavigationActivity : AppCompatActivity() {

    /* ----- Layout binding reference ----- */
    private lateinit var binding: LayoutActivityNavigationBinding

    /* ----- Mapbox Maps components ----- */
    private lateinit var mapboxMap: MapboxMap

    /* ----- Mapbox Navigation components ----- */
    private lateinit var mapboxNavigation: MapboxNavigation

    // location puck integration
    private val navigationLocationProvider = NavigationLocationProvider()

    // camera
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
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

    // trip progress bottom view
    private lateinit var tripProgressApi: MapboxTripProgressApi

    // voice instructions
    private var isVoiceInstructionsMuted = false
    private lateinit var maneuverApi: MapboxManeuverApi
    private lateinit var speechAPI: MapboxSpeechApi
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    // route line
    private lateinit var routeLineAPI: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var routeArrowView: MapboxRouteArrowView
    private val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()

    /* ----- Voice instruction callbacks ----- */
    private val voiceInstructionsObserver = object : VoiceInstructionsObserver {
        override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
            speechAPI.generate(
                voiceInstructions,
                speechCallback
            )
        }
    }

    private val voiceInstructionsPlayerCallback =
        object : MapboxNavigationConsumer<SpeechAnnouncement> {
            override fun accept(value: SpeechAnnouncement) {
                // remove already consumed file to free-up space
                speechAPI.clean(value)
            }
        }

    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    // play the instruction via fallback text-to-speech engine
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    // play the sound file from the external generator
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    /* ----- Location and route progress callbacks ----- */
    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            // not handled
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = keyPoints
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            // update the camera position to account for the progressed fragment of the route
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()

            // show arrow on the route line with the next maneuver
            val maneuverArrowResult = routeArrowAPI.addUpcomingManeuverArrow(routeProgress)
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
            }

            // update top maneuver instructions
            maneuverApi.getUpcomingManeuverList(
                routeProgress
            ) { maneuvers ->
                maneuvers.onValue {
                    binding.maneuverView.renderUpcomingManeuvers(it)
                }
            }
            val routeStepProgress = routeProgress.currentLegProgress?.currentStepProgress
            if (routeStepProgress != null) {
                maneuverApi.getStepDistanceRemaining(
                    routeStepProgress
                ) { distanceRemaining ->
                    distanceRemaining.onValue {
                        binding.maneuverView.renderDistanceRemaining(it)
                    }
                }
            }

            // update bottom trip progress summary
            binding.tripProgressView.render(tripProgressApi.getTripProgress(routeProgress))
        }
    }

    /* ----- Maneuver instruction callbacks ----- */
    private val bannerInstructionsObserver = BannerInstructionsObserver { bannerInstructions ->
        binding.maneuverView.visibility = VISIBLE
        maneuverApi.getManeuver(
            bannerInstructions
        ) { maneuver ->
            // updates the maneuver view whenever new data is available
            binding.maneuverView.renderManeuver(maneuver)
        }
    }

    private val routesObserver = RoutesObserver { routes ->
        if (routes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            CoroutineScope(Dispatchers.Main).launch {
                val result = routeLineAPI.setRoutes(
                    listOf(RouteLine(routes.first(), null))
                )
                val style = mapboxMap.getStyle()
                if (style != null) {
                    routeLineView.renderRouteDrawData(style, result)
                }
            }

            // update the camera position to account for the new route
            viewportDataSource.onRouteChanged(routes.first())
            viewportDataSource.evaluate()
        } else {
            // remove the route line and route arrow from the map
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeLineAPI.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowAPI.clearArrows())
            }

            // remove the route reference to change camera position
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()

        // initialize the location puck
        binding.mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MapboxNavigationActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }

        // initialize Mapbox Navigation
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .build()
        )
        // move the camera to current location on the first update
        mapboxNavigation.registerLocationObserver(object : LocationObserver {
            override fun onRawLocationChanged(rawLocation: Location) {
                val point = Point.fromLngLat(rawLocation.longitude, rawLocation.latitude)
                val cameraOptions = CameraOptions.Builder()
                    .center(point)
                    .zoom(13.0)
                    .build()
                mapboxMap.setCamera(cameraOptions)
                mapboxNavigation.unregisterLocationObserver(this)
            }

            override fun onEnhancedLocationChanged(
                enhancedLocation: Location,
                keyPoints: List<Location>
            ) {
                // not handled
            }
        })

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        )
        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        navigationCamera.registerNavigationCameraStateChangeObserver(
            object : NavigationCameraStateChangedObserver {
                override fun onNavigationCameraStateChanged(
                    navigationCameraState: NavigationCameraState
                ) {
                    // shows/hide the recenter button depending on the camera state
                    when (navigationCameraState) {
                        NavigationCameraState.TRANSITION_TO_FOLLOWING,
                        NavigationCameraState.FOLLOWING -> binding.recenter.visibility = INVISIBLE

                        NavigationCameraState.TRANSITION_TO_OVERVIEW,
                        NavigationCameraState.OVERVIEW,
                        NavigationCameraState.IDLE -> binding.recenter.visibility = VISIBLE
                    }
                }
            }
        )
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

        // initialize top maneuver view
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(DistanceFormatterOptions.Builder(this).build())
        )

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(this)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(
                        mapboxNavigation.navigationOptions.distanceFormatterOptions
                    )
                )
                .timeRemainingFormatter(TimeRemainingFormatter(this))
                .percentRouteTraveledFormatter(PercentDistanceTraveledFormatter())
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(this, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        // initialize voice instructions
        speechAPI = MapboxSpeechApi(
            this,
            getMapboxAccessTokenFromResources(),
            Locale.US.language
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            getMapboxAccessTokenFromResources(),
            Locale.US.language
        )

        // initialize route line
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        // load map style
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    // add long click listener that search for a route to the clicked destination
                    binding.mapView.gestures.addOnMapLongClickListener(
                        object : OnMapLongClickListener {
                            override fun onMapLongClick(point: Point): Boolean {
                                findRoute(point)
                                return true
                            }
                        }
                    )
                }
            }
        )

        // initialize view interactions
        binding.stop.setOnClickListener {
            clearRouteAndStopNavigation()
        }
        binding.recenter.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
        }
        binding.routeOverview.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenter.showTextAndExtend(2000L)
        }
        binding.soundButton.setOnClickListener {
            // mute/unmute voice instructions
            isVoiceInstructionsMuted = !isVoiceInstructionsMuted
            if (isVoiceInstructionsMuted) {
                binding.soundButton.muteAndExtend(2000L)
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                binding.soundButton.unmuteAndExtend(2000L)
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }

        // start the trip session to being receiving location updates in free drive
        // and later when a route is set, also receiving route progress updates
        mapboxNavigation.startTripSession()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver)
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        mapboxNavigation.onDestroy()
        speechAPI.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    private fun findRoute(destination: Point) {
        val origin = navigationLocationProvider.lastLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return

        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .coordinatesList(listOf(origin, destination))
                .build(),
            object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    setRouteAndStartNavigation(routes.first())
                }

                override fun onRoutesRequestFailure(
                    throwable: Throwable,
                    routeOptions: RouteOptions
                ) {
                    // no impl
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    // no impl
                }
            }
        )
    }

    private fun setRouteAndStartNavigation(route: DirectionsRoute) {
        // set route
        mapboxNavigation.setRoutes(listOf(route))

        // show UI elements
        binding.soundButton.visibility = VISIBLE
        binding.routeOverview.visibility = VISIBLE
        binding.tripProgressCard.visibility = VISIBLE
        binding.routeOverview.showTextAndExtend(2000L)
        binding.soundButton.unmuteAndExtend(2000L)

        // move the camera to overview when new route is available
        navigationCamera.requestNavigationCameraToOverview()
    }

    private fun clearRouteAndStopNavigation() {
        // clear
        mapboxNavigation.setRoutes(listOf())

        // hide UI elements
        binding.soundButton.visibility = INVISIBLE
        binding.maneuverView.visibility = INVISIBLE
        binding.routeOverview.visibility = INVISIBLE
        binding.tripProgressCard.visibility = INVISIBLE
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return Utils.getMapboxAccessToken(this)
    }
}
