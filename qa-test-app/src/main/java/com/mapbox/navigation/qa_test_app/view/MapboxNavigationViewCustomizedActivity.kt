package com.mapbox.navigation.qa_test_app.view

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.ViewOptionsCustomization.Companion.defaultRouteLineOptions
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.tripsession.TripSessionStarterAction
import com.mapbox.navigation.dropin.component.tripsession.TripSessionStarterViewModel
import com.mapbox.navigation.dropin.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewCustomizedBinding
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale

class MapboxNavigationViewCustomizedActivity : AppCompatActivity() {

    private val routeLineOptions: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
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
            .withRouteLineBelowLayerId("road-label-navigation")
            .withVanishingRouteLineEnabled(true)
            .displaySoftGradientForTraffic(true)
            .build()
    }

    private val showCustomViews = MutableLiveData(true)

    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .withArrowColor(Color.RED)
            .build()
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = LayoutActivityNavigationViewCustomizedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())

        // This demonstrates that you can customize views at any time. You can also reset to
        // the default views.
        showCustomViews.observe(this) { showCustomViews ->
            binding.toggleCustomViews.isChecked = showCustomViews
            if (showCustomViews) {
                binding.navigationView.customizeViewBinders {
                    speedLimit = CustomSpeedLimitViewBinder()
                }
                binding.navigationView.customizeViewOptions {
                    routeLineOptions = this@MapboxNavigationViewCustomizedActivity.routeLineOptions
                }
            } else {
                // Reset defaults
                binding.navigationView.customizeViewBinders {
                    speedLimit = UIBinder.USE_DEFAULT
                }
                binding.navigationView.customizeViewOptions {
                    routeLineOptions = defaultRouteLineOptions(applicationContext)
                }
            }
        }
        binding.toggleCustomViews.setOnClickListener {
            showCustomViews.value = showCustomViews.value?.not()
        }

        when (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.toggleTheme.isChecked = true
            }
            else -> {
                binding.toggleTheme.isChecked = false
            }
        }

        binding.toggleTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                toggleTheme(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                toggleTheme(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        binding.navigationView.customizeViewOptions {
            routeArrowOptions = this@MapboxNavigationViewCustomizedActivity.routeArrowOptions
        }

        // This is not the intended long term solution. We have not yet decided how
        // to expose view model actions. Considering that we need a navigationView to be created
        // before we can interact with the view models, we will probably prefer something like this:
        //       binding.navigationView.api.enableReplay();
        // TODO Make a ticket with link to list of public api needs
        val tripSessionStarterViewModel = MapboxNavigationApp
            .getObserver(TripSessionStarterViewModel::class)
        binding.toggleReplay.isChecked = tripSessionStarterViewModel.state.value.isReplayEnabled
        binding.toggleReplay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                tripSessionStarterViewModel.invoke(
                    TripSessionStarterAction.EnableReplayTripSession
                )
            } else {
                tripSessionStarterViewModel.invoke(
                    TripSessionStarterAction.EnableTripSession
                )
            }
        }
    }

    private fun toggleTheme(themeMode: Int) {
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class CustomSpeedLimitViewBinder : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val textView = TextView(viewGroup.context)
        textView.setBackgroundColor(Color.RED)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30.0f)
        TransitionManager.beginDelayedTransition(viewGroup, Fade())
        viewGroup.removeAllViews()
        viewGroup.addView(textView)
        return navigationCoroutine { mapboxNavigation ->
            mapboxNavigation.flowLocationMatcherResult().collect {
                val currentSpeed = it.enhancedLocation.speed
                val speedLimitKph = it.speedLimit?.speedKmph

                textView.text = when {
                    speedLimitKph == null && currentSpeed <= 0.0f ->
                        "You're not moving"
                    speedLimitKph == null && currentSpeed > 0.0 ->
                        "Current speed: ${currentSpeed.formatSpeed()}"
                    speedLimitKph != null && currentSpeed >= 0.0 -> """
                        Current speed: ${currentSpeed.formatSpeed()}
                        Speed limit: $speedLimitKph kmh
                    """.trimIndent()
                    else -> "UNKNOWN STATE: $currentSpeed $speedLimitKph"
                }
            }
        }
    }

    fun Float.formatSpeed(): String =
        String.format(Locale.US, "%.2f kmh", this)
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private fun navigationCoroutine(
    function: suspend CoroutineScope.(MapboxNavigation) -> Unit
) = object : UIComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            function(mapboxNavigation)
        }
    }
}
