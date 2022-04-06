package com.mapbox.navigation.qa_test_app.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ConstrainMode
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.GlyphsRasterizationOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.TileStoreUsageMode
import com.mapbox.maps.applyDefaultParams
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.ViewOptionsCustomization.Companion.defaultRouteLineOptions
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.tripsession.TripSessionStarterAction
import com.mapbox.navigation.dropin.component.tripsession.TripSessionStarterViewModel
import com.mapbox.navigation.dropin.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewCustomizedBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale

class CustomizedViewModel : ViewModel() {
    val showCustomViews = MutableLiveData(false)
    val showCustomMapView = MutableLiveData(false)
}

class MapboxNavigationViewCustomizedActivity : AppCompatActivity() {
    private val viewModel: CustomizedViewModel by viewModels()

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
            .withRouteLineBelowLayerId("road-label") // for Style.LIGHT and Style.DARK
            .withVanishingRouteLineEnabled(true)
            .displaySoftGradientForTraffic(true)
            .build()
    }

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
        viewModel.showCustomViews.observe(this) { showCustomViews ->
            binding.toggleCustomViews.isChecked = showCustomViews
            if (showCustomViews) {
                binding.navigationView.customizeViewBinders {
                    speedLimitBinder = CustomSpeedLimitViewBinder()
                }
                binding.navigationView.customizeViewOptions {
                    routeLineOptions = this@MapboxNavigationViewCustomizedActivity.routeLineOptions
                    mapStyleUriDay = Style.LIGHT
                    mapStyleUriNight = Style.DARK
                }
            } else {
                // Reset defaults
                binding.navigationView.customizeViewBinders {
                    speedLimitBinder = UIBinder.USE_DEFAULT
                }
                binding.navigationView.customizeViewOptions {
                    routeLineOptions = defaultRouteLineOptions(applicationContext)
                    mapStyleUriDay = NavigationStyles.NAVIGATION_DAY_STYLE
                    mapStyleUriNight = NavigationStyles.NAVIGATION_NIGHT_STYLE
                }
            }
        }

        binding.toggleCustomViews.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.showCustomViews.value = isChecked
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

        // Demonstrate map customization
        viewModel.showCustomMapView.observe(this) { showCustomMapView ->
            binding.toggleCustomMap.isChecked = showCustomMapView
            if (showCustomMapView) {
                binding.navigationView.customizeMapView(customMapViewFromCode(this))
            } else {
                binding.navigationView.customizeMapView(null)
            }
        }
        binding.toggleCustomMap.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.showCustomMapView.value = isChecked
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

private fun customMapViewFromCode(context: Context): MapView {
    // set map options
    val mapOptions = MapOptions.Builder().applyDefaultParams(context)
        .constrainMode(ConstrainMode.HEIGHT_ONLY)
        .glyphsRasterizationOptions(
            GlyphsRasterizationOptions.Builder()
                .rasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                .fontFamily("sans-serif")
                .build()
        )
        .build()

    // set token and cache size for this particular map view, these settings will overwrite
    // the default value.
    val resourceOptions = ResourceOptions.Builder().applyDefaultParams(context)
        .accessToken(Utils.getMapboxAccessToken(context))
        .tileStoreUsageMode(TileStoreUsageMode.DISABLED)
        .build()

    // set initial camera position
    val initialCameraOptions = CameraOptions.Builder()
        .center(Point.fromLngLat(-122.4194, 37.7749))
        .zoom(9.0)
        .bearing(120.0)
        .build()

    val mapInitOptions = MapInitOptions(
        context = context,
        resourceOptions = resourceOptions,
        mapOptions = mapOptions,
        cameraOptions = initialCameraOptions,
        textureView = true
    )

    // create view programmatically and add to root layout
    val customMapView = MapView(context, mapInitOptions)
    customMapView.getMapboxMap().loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE)
    return customMapView
}
