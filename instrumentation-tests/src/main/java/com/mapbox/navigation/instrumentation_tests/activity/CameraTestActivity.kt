package com.mapbox.navigation.instrumentation_tests.activity

import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadProgress
import com.mapbox.common.ResourceLoadResult
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.replay.route.ReplayRouteOptions
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadObserver
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.launch
import java.util.Locale
import com.mapbox.navigation.instrumentation_tests.databinding.LayoutActivityVoiceBinding
import kotlinx.coroutines.delay

class CameraTestActivity : AppCompatActivity() {

    private val routes = RoutesProvider.routes
    private var isMuted: Boolean = false

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var binding: LayoutActivityVoiceBinding
    private lateinit var locationComponent: LocationComponentPlugin

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * The [MapboxSpeechApi] consumes route progress and voice instructions data
     * and produces trip related data that is consumed by the [MapboxVoiceInstructionsPlayer]
     * in the form of speech.
     */
    private val speechApi: MapboxSpeechApi by lazy {
        MapboxSpeechApi(this, getMapboxAccessTokenFromResources(), Locale.US.toLanguageTag())
    }

    /**
     * The [MapboxVoiceInstructionsPlayer] consumes the voice instructions data
     * and plays them using the appropriate TTS player.
     */
    private val voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer by lazy {
        val options = VoiceInstructionsPlayerOptions.Builder()
            .abandonFocusDelay(PLAYER_ABANDON_FOCUS_DELAY)
            .build()
        MapboxVoiceInstructionsPlayer(
            this,
            getMapboxAccessTokenFromResources(),
            Locale.US.toLanguageTag(),
            options
        )
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

    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    /**
     * The result of invoking [MapboxVoiceInstructionsPlayer.play] is returned as a callback
     * containing [SpeechAnnouncement].
     */
    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value -> speechApi.clean(value) }

    /**
     * The result of invoking [MapboxSpeechApi.generate] is returned as a callback
     * containing either a success in the form of [SpeechValue] or failure in the form of
     * [SpeechError].
     */
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    logD("play(fallback): '${error.fallback.announcement}'", TAG)
                    // The data obtained in the form of an error is played using
                    // voiceInstructionsPlayer.
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    logD("play: '${value.announcement.announcement}'", TAG)
                    // The data obtained in the form of speech announcement is played using
                    // voiceInstructionsPlayer.
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
            ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                view.renderManeuverUpdate(style, this)
            }
        }
    }

    private val voiceInstructionsObserver =
        VoiceInstructionsObserver { voiceInstructions -> // The data obtained must be used to generate the synthesized speech mp3 file.
            speechApi.generate(
                voiceInstructions,
                speechCallback
            )
        }

    private val routesObserver =
        RoutesObserver { result -> // Every time a new route is obtained make sure to cancel the [MapboxSpeechApi] and
            // clear the [MapboxVoiceInstructionsPlayer]
            speechApi.cancel()
            voiceInstructionsPlayer.clear()
            if (result.navigationRoutes.isNotEmpty()) {
                lifecycleScope.launch {
                    routeLineApi.setNavigationRoutes(
                        listOf(result.navigationRoutes[0])
                    ).apply {
                        mapboxMap.getStyle()?.let {
                            routeLineView.renderRouteDrawData(it, this)
                        }
                    }
                }
                startSimulation(result.navigationRoutes[0].directionsRoute)
            }
        }

    @SuppressLint("MissingPermission")
    private fun init() {
        initNavigation()
        initStyle()
        initButtons()
        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutes(routes)
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        val navigationOptions = NavigationOptions.Builder(this)
            .accessToken(getMapboxAccessTokenFromResources())
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()
        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap()
        ).apply {
            followingPadding = EdgeInsets(180.0.dp, 40.0.dp, 150.0.dp, 40.0.dp)
        }

        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        ).apply {
            requestNavigationCameraToFollowing()
            registerNavigationCameraStateChangeObserver { navigationCameraState ->
                binding.recenterButton.isVisible = navigationCameraState in listOf(
                    NavigationCameraState.TRANSITION_TO_OVERVIEW,
                    NavigationCameraState.OVERVIEW,
                    NavigationCameraState.IDLE,
                )
            }
        }

        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
    }

    private fun initStyle() {
        mapboxMap.loadStyleUri(MAPBOX_STREETS) { style ->
            routeLineView.initializeLayers(style)
        }
    }

    private fun initButtons() {
        soundButtonMake(isMuted) // init state

        binding.soundButton.setOnClickListener {
            soundButtonMake(!isMuted)
        }

        binding.addPlay.setOnClickListener {
            voiceInstructionsPlayer.play(
                SpeechAnnouncement.Builder("Test hybrid speech player.").build(),
                voiceInstructionsPlayerCallback
            )
        }

        binding.recenterButton.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
        }
    }

    private fun soundButtonMake(mute: Boolean) {
        val muted = if (mute) {
            binding.soundButton.muteAndExtend(SOUND_BUTTON_TEXT_APPEAR_DURATION)
        } else {
            binding.soundButton.unmuteAndExtend(SOUND_BUTTON_TEXT_APPEAR_DURATION)
        }
        handleSoundState(muted)
    }

    private fun handleSoundState(value: Boolean) {
        if (value) {
            // This is used to set the speech volume to mute.
            voiceInstructionsPlayer.volume(SpeechVolume(0.0f))
        } else {
            // This is used to set the speech volume to max
            voiceInstructionsPlayer.volume(SpeechVolume(1.0f))
        }
        isMuted = value
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    @SuppressLint("MissingPermission")
    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = ReplayRouteMapper(ReplayRouteOptions.Builder().maxSpeedMps(22.222).build()).mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun startTesting() {
        lifecycleScope.launch {
            while (true) {
                delay(1)
            }
        }
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        init()
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        ResourceLoaderFactory.getInstance().registerObserver(resourceLoadObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        ResourceLoaderFactory.getInstance().unregisterObserver(resourceLoadObserver)
        if (this::mapboxNavigation.isInitialized) {
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        }

        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxReplayer.finish()
        if (this::mapboxNavigation.isInitialized) {
            mapboxNavigation.onDestroy()
        }
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    // ResourceLoadObserver that logs ResourceLoader operations
    private val resourceLoadObserver = object : ResourceLoadObserver {
        override fun onStart(request: ResourceLoadRequest) = Unit

        override fun onProgress(
            request: ResourceLoadRequest,
            progress: ResourceLoadProgress
        ) = Unit

        override fun onFinish(
            request: ResourceLoadRequest,
            result: Expected<ResourceLoadError, ResourceLoadResult>
        ) {
            result.value?.also { v ->
                val cleanURL = request.url.replace(
                    Regex("access_token=([a-zA-Z0-9.]+)"),
                    "access_token=REDACTED"
                )
                val values = mutableMapOf(
                    "status" to v.status,
                    "totalBytes" to v.totalBytes,
                    "transferredBytes" to v.transferredBytes,
                    "url" to cleanURL
                )
                logD("onFinish: $values", "ResourceLoadObserver")
            }
        }
    }

    private val Number.dp: Double get() = toDouble() * Resources.getSystem().displayMetrics.density

    private companion object {
        private const val TAG = "MapboxVoiceActivity"
        private const val SOUND_BUTTON_TEXT_APPEAR_DURATION = 1000L
        private const val PLAYER_ABANDON_FOCUS_DELAY = 2000L
    }
}
