package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
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
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityVoiceBinding
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
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
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * This activity demonstrates the usage of the [MapboxSpeechApi]. There is boiler plate
 * code for establishing basic navigation and a route simulator is used. The example assumes
 * that LOCATION permission has already been granted.
 *
 * The code specifically related to the voice component is commented in order to call
 * attention to its usage. Long press anywhere on the map to set a destination and trigger
 * navigation.
 */
class MapboxVoiceActivity : AppCompatActivity(), OnMapLongClickListener {

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
        MapboxSpeechApi(this, getMapboxAccessTokenFromResources(), Locale.US.language)
    }

    /**
     * The [MapboxVoiceInstructionsPlayer] consumes the voice instructions data
     * and plays them using the appropriate TTS player.
     */
    private val voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer by lazy {
        MapboxVoiceInstructionsPlayer(
            this,
            getMapboxAccessTokenFromResources(),
            Locale.US.language
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
                    // The data obtained in the form of an error is played using
                    // voiceInstructionsPlayer.
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
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
            updateCamera(locationMatcherResult.enhancedLocation)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
            ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                view.renderManeuverUpdate(style, this)
            }
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions -> // The data obtained must be used to generate the synthesized speech mp3 file.
        speechApi.generate(
            voiceInstructions,
            speechCallback
        )
    }

    private val routesObserver = RoutesObserver { routes -> // Every time a new route is obtained make sure to cancel the [MapboxSpeechApi] and
        // clear the [MapboxVoiceInstructionsPlayer]
        speechApi.cancel()
        voiceInstructionsPlayer.clear()
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

    private companion object {
        private const val SOUND_BUTTON_TEXT_APPEAR_DURATION = 1000L
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        initNavigation()
        initStyle()
        initButtons()
        mapboxNavigation.startTripSession()
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
    }

    private fun initStyle() {
        mapboxMap.loadStyleUri(MAPBOX_STREETS) { style ->
            routeLineView.initializeLayers(style)
            binding.mapView.gestures.addOnMapLongClickListener(this)
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

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun findRoute(origin: Point, destination: Point) {
        val routeOptions: RouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinatesList(listOf(origin, destination))
            .voiceInstructions(true)
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

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        binding.mapView.camera.easeTo(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        init()
    }

    override fun onStart() {
        super.onStart()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    override fun onMapLongClick(point: Point): Boolean {
        val currentLocation = navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, point)
        }
        return false
    }
}
