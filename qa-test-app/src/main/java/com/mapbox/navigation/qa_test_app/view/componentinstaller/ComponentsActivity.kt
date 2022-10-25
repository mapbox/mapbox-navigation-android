package com.mapbox.navigation.qa_test_app.view.componentinstaller

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.qa_test_app.databinding.ComponentsActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.lifecycle.DropInStartReplayButton
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationViewModel
import com.mapbox.navigation.qa_test_app.utils.Utils.getMapboxAccessToken
import com.mapbox.navigation.qa_test_app.view.componentinstaller.components.FindRouteOnLongPress
import com.mapbox.navigation.qa_test_app.view.customnavview.dp
import com.mapbox.navigation.ui.base.installer.installComponents
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

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ComponentsActivity : AppCompatActivity() {

    private val binding: ComponentsActivityLayoutBinding by lazy {
        ComponentsActivityLayoutBinding.inflate(layoutInflater)
    }

    private val viewModel: DropInNavigationViewModel by viewModels()

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
                viewportDataSource = cameraViewportDataSource()
            }

            // custom components
            component(FindRouteOnLongPress(binding.mapView))
            component(DropInStartReplayButton(binding.startNavigation))
        }
    }

    private fun cameraViewportDataSource(): MapboxNavigationViewportDataSource {
        return MapboxNavigationViewportDataSource(
            mapboxMap = binding.mapView.getMapboxMap()
        ).apply {
            val insets = EdgeInsets(0.0, 0.0, 100.dp.toDouble(), 0.0)
            overviewPadding = insets
            followingPadding = insets
        }
    }
}
