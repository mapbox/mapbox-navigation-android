package com.mapbox.navigation.examples.androidauto.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.lifecycleScope
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.examples.androidauto.CarAppSyncComponent
import com.mapbox.navigation.examples.androidauto.databinding.ActivityMainBinding
import com.mapbox.navigation.examples.androidauto.databinding.LayoutDrawerMenuNavViewBinding
import com.mapbox.navigation.examples.androidauto.utils.NavigationViewController
import com.mapbox.navigation.examples.androidauto.utils.TestRoutes
import com.mapbox.navigation.ui.base.installer.installComponents
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxJunctionApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : DrawerActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuBinding: LayoutDrawerMenuNavViewBinding

    override fun onCreateContentView(): View {
        binding = ActivityMainBinding.inflate(layoutInflater)
        CarAppSyncComponent.getInstance().attachNavigationView(binding.navigationView)
        return binding.root
    }

    override fun onCreateMenuView(): View {
        menuBinding = LayoutDrawerMenuNavViewBinding.inflate(layoutInflater)
        return menuBinding.root
    }

    private lateinit var controller: NavigationViewController

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LogConfiguration.setLoggingLevel("nav-sdk", LoggingLevel.DEBUG)

        controller = NavigationViewController(this, binding.navigationView)

        menuBinding.toggleReplay.isChecked = binding.navigationView.api.isReplayEnabled()
        menuBinding.toggleReplay.setOnCheckedChangeListener { _, isChecked ->
            binding.navigationView.api.routeReplayEnabled(isChecked)
        }

        menuBinding.junctionViewTestButton.setOnClickListener {
            lifecycleScope.launch {
                val (origin, destination) = TestRoutes.valueOf(
                    menuBinding.spinnerTestRoute.selectedItem as String
                )
                controller.startActiveGuidance(origin, destination)
                closeDrawers()
            }
        }

        MapboxNavigationApp.installComponents(this) {
            component(Junctions(binding.junctionImageView))
        }
    }

    /**
     * Simple component for detecting and rendering Junction Views.
     */
    private class Junctions(
        private val imageView: AppCompatImageView
    ) : UIComponent() {
        private var junctionApi: MapboxJunctionApi? = null

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            super.onAttached(mapboxNavigation)
            val token = mapboxNavigation.navigationOptions.accessToken!!
            junctionApi = MapboxJunctionApi(token)

            mapboxNavigation.flowBannerInstructions().observe { instructions ->
                junctionApi?.generateJunction(instructions) { result ->
                    result.fold(
                        { imageView.setImageBitmap(null) },
                        { imageView.setImageBitmap(it.bitmap) }
                    )
                }
            }
            mapboxNavigation.flowRoutesUpdated().observe {
                if (it.navigationRoutes.isEmpty()) {
                    imageView.setImageBitmap(null)
                }
            }
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            super.onDetached(mapboxNavigation)
            junctionApi?.cancelAll()
            junctionApi = null
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun MapboxNavigation.flowBannerInstructions(): Flow<BannerInstructions> =
            callbackFlow {
                val observer = BannerInstructionsObserver { trySend(it) }
                registerBannerInstructionsObserver(observer)
                awaitClose { unregisterBannerInstructionsObserver(observer) }
            }
    }
}
