package com.mapbox.navigation.qa_test_app.view

import android.location.Location
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowNewRawLocation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.ComponentsActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.lifecycle.DropInStartReplayButton
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInLocationViewModel
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationViewModel
import com.mapbox.navigation.qa_test_app.utils.Utils.getMapboxAccessToken
import com.mapbox.navigation.qa_test_app.view.customnavview.dp
import com.mapbox.navigation.ui.base.installer.installComponents
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.installer.cameraModeButton
import com.mapbox.navigation.ui.maps.installer.locationPuck
import com.mapbox.navigation.ui.maps.installer.navigationCamera
import com.mapbox.navigation.ui.maps.installer.recenterButton
import com.mapbox.navigation.ui.maps.installer.roadName
import com.mapbox.navigation.ui.maps.installer.routeArrow
import com.mapbox.navigation.ui.maps.installer.routeLine
import com.mapbox.navigation.ui.voice.installer.audioGuidanceButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ComponentsActivity : AppCompatActivity() {

    private val binding: ComponentsActivityLayoutBinding by lazy {
        ComponentsActivityLayoutBinding.inflate(layoutInflater)
    }

    private val viewModel: DropInNavigationViewModel by viewModels()
    private val locationViewModel: DropInLocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewModel.triggerIdleCameraOnMoveListener = false

        binding.mapView.getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE)

        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(this)
                    .accessToken(getMapboxAccessToken(this))
                    .build()
            )
        }

        // Add active guidance banner with maneuverView and trip progress in a Fragment.
        // This example shows a retained fragment only because they are more complicated and
        // we wanted to ensure our framework supports it.
        val currentFragment = supportFragmentManager
            .findFragmentById(R.id.activeGuidanceBannerFragment)
        if (currentFragment !is RetainedActiveGuidanceFragment) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<RetainedActiveGuidanceFragment>(R.id.activeGuidanceBannerFragment)
            }
        }

        val dataSource = MapboxNavigationViewportDataSource(
            mapboxMap = binding.mapView.getMapboxMap()
        ).apply {
            val insets = EdgeInsets(0.0, 0.0, 100.dp.toDouble(), 0.0)
            overviewPadding = insets
            followingPadding = insets
        }

        //
        // Components installation via MapboxNavigationApp Facade
        //
        MapboxNavigationApp.installComponents(this) {
            audioGuidanceButton(binding.soundButton)
            locationPuck(binding.mapView)
            routeLine(binding.mapView)
            routeArrow(binding.mapView)
            roadName(binding.mapView, binding.roadNameView)
            recenterButton(binding.mapView, binding.recenterButton)

            cameraModeButton(binding.cameraModeButton)
            navigationCamera(binding.mapView) {
                viewportDataSource = dataSource
            }

            // custom components
            component(FindRouteOnLongPress(binding.mapView))
            component(DropInStartReplayButton(binding.startNavigation))
        }
    }
}

@ExperimentalPreviewMapboxNavigationAPI
class FindRouteOnLongPress(
    val mapView: MapView
) : UIComponent(), OnMapLongClickListener {

    private var originLocation: Location? = null
    private var mapboxNavigation: MapboxNavigation? = null

    override fun onMapLongClick(point: Point): Boolean {
        findRoute(point)
        return false
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation

        coroutineScope.launch {
            originLocation = runCatching {
                val locationEngine = mapboxNavigation.navigationOptions.locationEngine
                locationEngine.getLastLocation()
            }.getOrNull()

            mapboxNavigation.flowNewRawLocation().collect {
                originLocation = it
            }
        }

        mapView.gestures.addOnMapLongClickListener(this)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.gestures.removeOnMapLongClickListener(this)
        this.mapboxNavigation = null
    }

    private fun findRoute(destination: Point) = mapboxNavigation?.also { mapboxNavigation ->
        val origin = originLocation?.run { Point.fromLngLat(longitude, latitude) } ?: return@also

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(mapView.context)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setRoutes(routes.reversed())
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) = Unit

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    routerOrigin: RouterOrigin
                ) = Unit
            }
        )
    }

    @Throws(SecurityException::class)
    private suspend fun LocationEngine.getLastLocation() =
        suspendCancellableCoroutine<Location> { cont ->
            getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult) {
                    result.lastLocation?.also {
                        cont.resume(it)
                    }
                }

                override fun onFailure(exception: Exception) {
                    cont.resumeWithException(exception)
                }
            })
        }
}
