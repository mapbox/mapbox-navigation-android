package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.AppLifecycleActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.lifecycle.DropInCameraMode
import com.mapbox.navigation.qa_test_app.lifecycle.DropInContinuousRoutes
import com.mapbox.navigation.qa_test_app.lifecycle.DropInLocationComponent
import com.mapbox.navigation.qa_test_app.lifecycle.DropInLocationViewModel
import com.mapbox.navigation.qa_test_app.lifecycle.DropInNavigationCamera
import com.mapbox.navigation.qa_test_app.lifecycle.DropInReplayViewModel
import com.mapbox.navigation.qa_test_app.lifecycle.DropInRoutesInteractor
import com.mapbox.navigation.qa_test_app.utils.Utils.getMapboxAccessToken
import com.mapbox.navigation.ui.maps.NavigationStyles

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AppLifecycleActivity : AppCompatActivity() {

    private val binding: AppLifecycleActivityLayoutBinding by lazy {
        AppLifecycleActivityLayoutBinding.inflate(layoutInflater)
    }

    private val replayViewModel: DropInReplayViewModel by viewModels()
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
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessToken(this))
                .build()
        )

        // This is commented out to show that MapboxNavigationApp.attach will work when using a
        // fragment as well. Look into the `RetainedActiveGuidanceFragment` to see it also attaches
        // itself. In real scenarios, you should attach both LifecycleOwners for better code
        // maintainability!
        // MapboxNavigationApp.attach(this)

        // Setup location. The locationViewModel is separate so it can be shared between
        // the camera, location puck, and the long click listener. The LocationViewModel
        // will also survive orientation changes.
        lifecycle.addObserver(DropInLocationComponent(binding.mapView, locationViewModel))

        // Setup the navigation camera.
        lifecycle.addObserver(
            DropInNavigationCamera(
                binding.mapView,
                DropInCameraMode.FOLLOWING,
                locationViewModel
            )
        )

        // Setup routes, notice it is not a ViewModel because the state is held by
        // MapboxNavigation. Here we are re-attaching view state whenever an Activity is created.
        val routesInteractor = DropInRoutesInteractor(binding.mapView)
        lifecycle.addObserver(routesInteractor)

        // Setup a long press to find a route.
        binding.mapView.gestures.addOnMapLongClickListener { point ->
            vibrate()
            val originLocation = locationViewModel.locationLiveData.value
            routesInteractor.findRoute(originLocation, point)
            false
        }

        // Setup the map press to select alternative routes.
        binding.mapView.gestures.addOnMapClickListener { point ->
            routesInteractor.selectRoute(point)
            false
        }

        // Setup continuous alternative routes.
        lifecycle.addObserver(DropInContinuousRoutes())

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

        // Setup the navigation button.
        if (TripSessionState.STARTED != MapboxNavigationApp.current()?.getTripSessionState()) {
            binding.startNavigation.setOnClickListener {
                binding.startNavigation.visibility = View.GONE
                replayViewModel.startSimulation()
            }
        } else {
            binding.startNavigation.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100L)
        }
    }
}
