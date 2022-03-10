package com.mapbox.navigation.examples.core

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationUIBinders
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.examples.core.databinding.LayoutActivityNavigationViewCustomizedBinding
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale

class MapboxNavigationViewCustomizedActivity : AppCompatActivity() {

    private val routeLineOptions: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(RouteLineResources.Builder().build())
            .withRouteLineBelowLayerId("road-label-navigation")
            .withVanishingRouteLineEnabled(true)
            .displaySoftGradientForTraffic(true)
            .build()
    }

    private val showCustomViews = MutableLiveData(true)

    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = LayoutActivityNavigationViewCustomizedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, binding.navigationView.getOnBackPressedCallback())

        // This demonstrates that you can customize views at any time. You can also reset to
        // the default views.
        showCustomViews.observe(this) { showCustomViews ->
            if (showCustomViews) {
                binding.navigationView.customize(
                    NavigationUIBinders(
                        speedLimit = CustomSpeedLimitViewBinder()
                    )
                )
            } else {
                // Reset defaults
                binding.navigationView.customize(NavigationUIBinders())
            }
        }
        binding.toggleCustomViews.setOnClickListener {
            showCustomViews.value = showCustomViews.value?.not()
        }

        binding.navigationView.customize(routeLineOptions)
        binding.navigationView.customize(routeArrowOptions)
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
