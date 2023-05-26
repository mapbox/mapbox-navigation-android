package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.telemetry.events.UserFeedback
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.qa_test_app.databinding.FeedbackActivityBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.util.ViewUtils.capture
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class FeedbackActivity : AppCompatActivity() {

    private val binding: FeedbackActivityBinding by lazy {
        FeedbackActivityBinding.inflate(layoutInflater)
    }

    private val navigationLocationProvider = NavigationLocationProvider()

    private val mapCamera: CameraAnimationsPlugin by lazy {
        binding.mapView.camera
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) = Unit

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            updateCamera(locationMatcherResult.enhancedLocation, locationMatcherResult.keyPoints)
        }
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder().build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routesObserver = RoutesObserver { result ->
        val routelines = result.routes.map { RouteLine(it, null) }
        CoroutineScope(Dispatchers.Main).launch {
            routeLineApi.setRoutes(routelines).apply {
                routeLineView.renderRouteDrawData(
                    binding.mapView.getMapboxMap().getStyle()!!,
                    this
                )
            }
        }
    }

    private val sharedPrefs: SharedPreferences by lazy {
        getSharedPreferences("qa_feedback_activity", MODE_PRIVATE)
    }

    private val spListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_SP_FEEDBACK_METADATA || key == KEY_SP_FEEDBACK_SCREENSHOT) {
            checkStoredFeedbackMetadata()
        }
    }

    private var feedbackMetadataWrapper: FeedbackMetadataWrapper? = null
        set(value) {
            field = value
            enableButtonStoreFeedbackMetadata(value != null && feedbackScreenshot != null)
        }

    private var feedbackScreenshot: String? = null
        set(value) {
            field = value
            enableButtonStoreFeedbackMetadata(feedbackMetadataWrapper != null && value != null)
        }

    private val mapboxNavigation by requireMapboxNavigation(
        onCreatedObserver = object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                binding.mapView.location.apply {
                    setLocationProvider(navigationLocationProvider)
                    enabled = true
                }
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.registerRoutesObserver(routesObserver)
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
            }
        }
    ) {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .build(),
        )
    }

    private companion object {
        private const val KEY_SP_FEEDBACK_METADATA = "KEY_SP_FEEDBACK_METADATA"
        private const val KEY_SP_FEEDBACK_SCREENSHOT = "KEY_SP_FEEDBACK_SCREENSHOT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initStyle()
        initListeners()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        binding.mapView.getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE) {
            mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                object : LocationEngineCallback<LocationEngineResult> {
                    override fun onSuccess(result: LocationEngineResult) {
                        result.lastLocation?.let {
                            updateCamera(it, emptyList())
                        }
                    }

                    override fun onFailure(exception: Exception) = Unit
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        binding.mapView.getMapboxMap().addOnMapLongClickListener { point ->
            navigationLocationProvider.lastLocation?.let { lastLocation ->
                requestsRoute(lastLocation.toPoint(), point)
            }
            return@addOnMapLongClickListener true
        }

        binding.startNavigation.setOnClickListener {
            mapboxNavigation.startTripSession()
            binding.startNavigation.visibility = View.GONE
        }
        binding.positioningIssueFeedback.setOnClickListener {
            binding.mapView.capture { screenshot ->
                mapboxNavigation.postUserFeedback(
                    FeedbackEvent.POSITIONING_ISSUE,
                    "Test feedback",
                    FeedbackEvent.UI,
                    FeedbackHelper.encodeScreenshot(screenshot),
                )
            }
        }
        binding.takeFeedbackMetadata.setOnClickListener {
            if (mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
                feedbackMetadataWrapper = mapboxNavigation.provideFeedbackMetadataWrapper()
                binding.mapView.capture { screenshot ->
                    feedbackScreenshot = FeedbackHelper.encodeScreenshot(screenshot)
                }
            } else {
                Toast.makeText(
                    this,
                    "FeedbackMetadata is available when trip session is started",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.saveTakenFeedbackMetadata.setOnClickListener {
            val feedbackMetadata = feedbackMetadataWrapper?.get() ?: return@setOnClickListener
            val screenshot = feedbackScreenshot ?: return@setOnClickListener
            storeFeedbackMetadata(feedbackMetadata, screenshot)
        }
        binding.positioningIssueFeedbackWithMetadata.setOnClickListener {
            sharedPrefs.getString(KEY_SP_FEEDBACK_METADATA, null)?.let { json ->
                val feedbackMetadata = FeedbackMetadata.fromJson(json) ?: run {
                    Toast.makeText(
                        this,
                        "Cannot deserialize FeedbackMetadata from json",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@let
                }
                val screenshot = sharedPrefs.getString(KEY_SP_FEEDBACK_SCREENSHOT, null) ?: run {
                    Toast.makeText(
                        this,
                        "Cannot load feedback screenshot from preferences",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@let
                }
                mapboxNavigation.postUserFeedback(
                    UserFeedback.Builder(
                        FeedbackEvent.POSITIONING_ISSUE,
                        "Test feedback",
                    )
                        .feedbackMetadata(feedbackMetadata)
                        .build()

                )
                Toast.makeText(
                    this,
                    "Stored feedback has been sent",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.cleanUpFeedbackMetadata.setOnClickListener {
            feedbackMetadataWrapper = null
            cleanUpFeedbackMetadata()
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(spListener)
    }

    private fun updateCamera(location: Location, keyPoints: List<Location>) {
        navigationLocationProvider.changePosition(location, keyPoints, null, null)
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

    private fun requestsRoute(origin: Point, destination: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(listOf(origin, destination))
                .layersList(listOf(mapboxNavigation.getZLevel(), null))
                .alternatives(true)
                .build(),
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setRoutes(routes)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Toast.makeText(
                        this@FeedbackActivity,
                        "route request failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    Toast.makeText(
                        this@FeedbackActivity,
                        "route request canceled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun enableButtonStoreFeedbackMetadata(enable: Boolean) {
        binding.saveTakenFeedbackMetadata.isEnabled = enable
    }

    private fun checkStoredFeedbackMetadata() {
        val hasStoredMetadata = sharedPrefs.getString(KEY_SP_FEEDBACK_METADATA, null) != null &&
            sharedPrefs.getString(KEY_SP_FEEDBACK_SCREENSHOT, null) != null

        binding.cleanUpFeedbackMetadata.isEnabled = hasStoredMetadata
        binding.positioningIssueFeedbackWithMetadata.isEnabled = hasStoredMetadata
    }

    private fun storeFeedbackMetadata(feedbackMetadata: FeedbackMetadata, screenshot: String) {
        sharedPrefs.edit()
            .putString(KEY_SP_FEEDBACK_METADATA, feedbackMetadata.toJson(Gson()))
            .putString(KEY_SP_FEEDBACK_SCREENSHOT, screenshot)
            .apply()
    }

    private fun cleanUpFeedbackMetadata() {
        sharedPrefs.edit()
            .remove(KEY_SP_FEEDBACK_METADATA)
            .remove(KEY_SP_FEEDBACK_SCREENSHOT)
            .apply()
    }
}
