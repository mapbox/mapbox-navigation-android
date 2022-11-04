package com.mapbox.navigation.qa_test_app.view.componentinstaller

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowTripSessionState
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.ComponentsActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.lifecycle.DropInStartReplayButton
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationViewModel
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInReplayComponent
import com.mapbox.navigation.qa_test_app.utils.Utils.getMapboxAccessToken
import com.mapbox.navigation.qa_test_app.view.componentinstaller.components.FindRouteOnLongPress
import com.mapbox.navigation.qa_test_app.view.customnavview.dp
import com.mapbox.navigation.ui.base.installer.Installation
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
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.voice.installer.audioGuidanceButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoadObjectsMappingTestingActivity : AppCompatActivity() {

    private val binding: ComponentsActivityLayoutBinding by lazy {
        ComponentsActivityLayoutBinding.inflate(layoutInflater)
    }

    private val viewModel: DropInNavigationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewModel.triggerIdleCameraOnMoveListener = false

        binding.mapView.getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE)
        binding.startNavigation.isVisible = false
        binding.zoomToRomeButton.isVisible = true

        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(this)
                    .accessToken(getMapboxAccessToken(this))
                    .build()
            )
        }
    }

    override fun onStart() {
        super.onStart()

        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)

        //
        // Components installation via existing MapboxNavigation instance
        //
        val mapboxNavigation = MapboxNavigationApp.current()!!
        mapboxNavigation.installComponents(this) {
            audioGuidanceButton(binding.soundButton)
            locationPuck(binding.mapView)
            routeLine(binding.mapView) {
                options = MapboxRouteLineOptions.Builder(applicationContext)
                    .withRouteLineResources(RouteLineResources.Builder().build())
                    .withRouteLineBelowLayerId("road-label-navigation")
                    .withVanishingRouteLineEnabled(true)
                    .build()
            }
            routeArrow(binding.mapView)
            roadName(binding.mapView, binding.roadNameView)
            recenterButton(binding.mapView, binding.recenterButton)
            cameraModeButton(binding.cameraModeButton)
            navigationCamera(binding.mapView) {
                switchToIdleOnMapGesture = true
                viewportDataSource = cameraViewportDataSource()
            }

            // custom components
            component(StartReplaySession())
            component(FindRouteOnLongPress(binding.mapView) { customizeOptions() })
            component(ZoomToRomeButton(binding.mapView, binding.zoomToRomeButton))
        }
    }

    private class ZoomToRomeButton(
        val mapView: MapView,
        val button: View
    ): UIComponent() {
        private val ROME = Point.fromLngLat(
            12.483838237364807,
            41.88237804754755
        )

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            super.onAttached(mapboxNavigation)
            button.setOnClickListener {
                mapView.camera.flyTo(
                    CameraOptions.Builder()
                    .center(ROME)
                    .zoom(12.0)
                    .build()
                )
            }
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            super.onDetached(mapboxNavigation)
            button.setOnClickListener(null)
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    class StartReplaySession : UIComponent() {

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            super.onAttached(mapboxNavigation)

            MapboxNavigationApp.getObserver(DropInReplayComponent::class).startSimulation()
            mapboxNavigation.startReplayTripSession()


        }
    }

    private fun RouteOptions.Builder.customizeOptions() {
        profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .voiceInstructions(true)
            .bannerInstructions(true)
            .steps(true)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .annotationsList(listOf(
                DirectionsCriteria.ANNOTATION_CLOSURE,
                DirectionsCriteria.ANNOTATION_CONGESTION,
                DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                DirectionsCriteria.ANNOTATION_DURATION,
                DirectionsCriteria.ANNOTATION_SPEED,
                DirectionsCriteria.ANNOTATION_DISTANCE,
                DirectionsCriteria.ANNOTATION_MAXSPEED,
            ))
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .alternatives(true)
            .continueStraight(false)
            .roundaboutExits(true)
    }

    private fun cameraViewportDataSource(): MapboxNavigationViewportDataSource {
        return MapboxNavigationViewportDataSource(
            mapboxMap = binding.mapView.getMapboxMap()
        ).apply {
            val insets = EdgeInsets(100.0.dp.toDouble(), 0.0, 200.dp.toDouble(), 0.0)
            overviewPadding = insets
            followingPadding = insets
        }
    }

    var routeArrowInstallation: Installation? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_components, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_uninstall_arrow -> {
                // an example of early component uninstall
                routeArrowInstallation?.uninstall()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
