package com.mapbox.navigation.qa_test_app.view

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.notification.TripNotificationInterceptor
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.internal.extensions.attachCreated
import com.mapbox.navigation.dropin.internal.extensions.attachResumed
import com.mapbox.navigation.dropin.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.AppLifecycleActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.lifecycle.DropInContinuousRoutes
import com.mapbox.navigation.qa_test_app.lifecycle.DropInLocationPuck
import com.mapbox.navigation.qa_test_app.lifecycle.DropInNavigationCamera
import com.mapbox.navigation.qa_test_app.lifecycle.DropInRoutesInteractor
import com.mapbox.navigation.qa_test_app.lifecycle.DropInStartReplayButton
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInLocationViewModel
import com.mapbox.navigation.qa_test_app.lifecycle.viewmodel.DropInNavigationViewModel
import com.mapbox.navigation.qa_test_app.utils.Utils.getMapboxAccessToken
import com.mapbox.navigation.ui.maps.NavigationStyles
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AppLifecycleActivity : AppCompatActivity() {

    private val binding: AppLifecycleActivityLayoutBinding by lazy {
        AppLifecycleActivityLayoutBinding.inflate(layoutInflater)
    }

    private val viewModel: DropInNavigationViewModel by viewModels()
    private val locationViewModel: DropInLocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Load the map. This is good to put first, because the first thing
        // we want to see is the map.
        binding.mapView.getMapboxMap().loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE
        ) {
            // no op
        }

        /**
         * 1. MapboxNavigationApp.setup with your NavigationOptions.
         *    This is ignored when the configuration is changing (portrait|landscape switching).
         * 2. MapboxNavigationApp.attach this lifecycle to let the observers register to
         *    MapboxNavigation observers.
         * 2. Register navigation lifecycle components
         */

        // 1 and 2, setup and attach. The order of these functions does not matter.
        // But, they are needed before any MapboxNavigationObserver will call onAttached.
        // Consider moving this to the setup to your Application.onCreate, that way
        // MapboxNavigation can be created whenever you attach a LifecycleOwner.
        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(this)
                    .accessToken(getMapboxAccessToken(this))
                    .build()
            )
        }

        /**
         * Idea for interaction customization.
         */
        viewModel.triggerIdleCameraOnMoveListener = false

        attachCreated(CustomTripNotificationComponent())
        attachResumed(
            DropInLocationPuck(locationViewModel, binding.mapView),
            DropInRoutesInteractor(locationViewModel, binding.mapView),
            DropInNavigationCamera(viewModel, locationViewModel, this, binding.mapView),
            DropInContinuousRoutes(),
            DropInStartReplayButton(binding.startNavigation),
        )

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
}

/**
 * TODO Planning to remove this after adding an example that extends the notification to support
 *   Android Auto. For now, this is an example showing you can modify the notification at runtime.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class CustomTripNotificationComponent : UIComponent() {
    private var currentColor: Int? = null

    private val interceptor = TripNotificationInterceptor { builder ->
        currentColor?.let { builder.setColor(it) } ?: builder
    }

    private fun updateColor(routes: List<NavigationRoute>) {
        currentColor = when (routes.isEmpty()) {
            true -> Color.RED
            else -> null
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mapboxNavigation.setTripNotificationInterceptor(interceptor)
        coroutineScope.launch {
            updateColor(mapboxNavigation.getNavigationRoutes())
            mapboxNavigation.flowRoutesUpdated().collect { routesUpdated ->
                updateColor(routesUpdated.navigationRoutes)
            }
        }.invokeOnCompletion {
            mapboxNavigation.setTripNotificationInterceptor(null)
            currentColor = null
        }
    }
}
