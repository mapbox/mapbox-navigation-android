package com.mapbox.navigation.examples.voicefeedback

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.examples.core.databinding.LayoutActivityVoiceFeedbackBinding
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.base.installer.installComponents
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.voicefeedback.voiceFeedbackButton

/**
 * Example activity demonstrating the [libnavui-voicefeedback](https://docs.mapbox.com/android/navigation/ui/latest/libnavui-voicefeedback/overview/)
 * SDK. This SDK provides voice feedback capabilities, allowing users to report issues,
 * suggest improvements, or give feedback directly through voice while navigating.
 *
 * It utilizes [MapboxVoiceFeedbackButton] and [FeedbackAgentSession] (via the ComponentInstaller)
 * to manage the lifecycle and UI states of the voice interaction flow.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxVoiceFeedbackActivity : AppCompatActivity() {

    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var binding: LayoutActivityVoiceFeedbackBinding
    private lateinit var mapboxMap: MapboxMap
    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
            }
        },
        onInitialize = this::initNavigation
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val areGranted = permissions.values.all { it }
        if (areGranted) {
            mapboxNavigation.startTripSession()
        } else {
            Toast.makeText(this, "Location and Microphone permissions are required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityVoiceFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapboxMap = binding.mapView.getMapboxMap()

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    private fun initNavigation() {
        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(this)
                    .accessToken(Utils.getMapboxAccessToken(this))
                    .build()
            )
        }

        MapboxNavigationApp.installComponents(this) {
            voiceFeedbackButton(binding.voiceFeedbackButton)
        }

        binding.mapView.location.apply {
            enabled = true
            setLocationProvider(navigationLocationProvider)
        }

        mapboxMap.setCamera(
            com.mapbox.maps.CameraOptions.Builder()
                .center(com.mapbox.geojson.Point.fromLngLat(-73.98513, 40.748817)) // New York City
                .zoom(13.0)
                .build()
        )
    }

}
