package com.mapbox.navigation.qa_test_app.view

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.ComponentsActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.lifecycle.DropInLocationPuck
import com.mapbox.navigation.qa_test_app.lifecycle.DropInNavigationCamera
import com.mapbox.navigation.qa_test_app.lifecycle.DropInStartReplayButton
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInLocationViewModel
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationViewModel
import com.mapbox.navigation.qa_test_app.utils.Utils.getMapboxAccessToken
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.base.installer.installComponents
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.installer.routeArrow
import com.mapbox.navigation.ui.maps.installer.routeLine
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.voice.installer.audioGuidanceButton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ComponentsAltActivity : AppCompatActivity() {

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
    }

    override fun onStart() {
        super.onStart()

        //
        // Components installation via existing MapboxNavigation instance
        //
        installComponents {
            audioGuidanceButton(binding.soundButton)
            routeLine(binding.mapView) {
                options = customRouteLineOptions()
            }
            routeArrowInstallation = routeArrow(binding.mapView) {
                options = customRouteArrowOptions()
            }

            // custom components
            component(FindRouteOnLongPress(binding.mapView))
            component(DropInStartReplayButton(binding.startNavigation))
            component(
                DropInLocationPuck(
                    binding.mapView,
                    locationViewModel.navigationLocationProvider
                )
            )
            component(
                DropInNavigationCamera(
                    viewModel,
                    locationViewModel,
                    this@ComponentsAltActivity,
                    binding.mapView
                )
            )
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

    private fun customRouteLineOptions() = MapboxRouteLineOptions.Builder(applicationContext)
        .withRouteLineBelowLayerId("road-label-navigation")
        .withRouteLineResources(
            RouteLineResources.Builder()
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .routeLowCongestionColor(Color.YELLOW)
                        .routeCasingColor(Color.RED)
                        .build()
                )
                .build()
        )
        .withVanishingRouteLineEnabled(true)
        .displaySoftGradientForTraffic(true)
        .build()

    private fun customRouteArrowOptions() = RouteArrowOptions.Builder(applicationContext)
        .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
        .withArrowColor(Color.RED)
        .build()
}
