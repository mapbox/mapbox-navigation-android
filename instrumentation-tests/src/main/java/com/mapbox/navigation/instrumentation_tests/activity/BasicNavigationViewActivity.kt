package com.mapbox.navigation.instrumentation_tests.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.delegates.listeners.eventdata.MapLoadErrorType
import com.mapbox.navigation.instrumentation_tests.databinding.ActivityBasicNavigationViewBinding

class BasicNavigationViewActivity : AppCompatActivity() {

    lateinit var binding: ActivityBasicNavigationViewBinding
    lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBasicNavigationViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        mapboxMap.loadStyleUri(
            "asset://map_style_blank.json",
            onMapLoadErrorListener = object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapLoadErrorType: MapLoadErrorType, msg: String) {
                    Log.e(
                        "onMapLoadError",
                        "Error loading map - error type: $mapLoadErrorType, message: $msg"
                    )
                }
            }
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        binding.mapView.onDestroy()
        super.onDestroy()
    }
}
