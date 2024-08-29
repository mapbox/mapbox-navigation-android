package com.mapbox.navigation.instrumentation_tests.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.instrumentation_tests.databinding.ActivityBasicNavigationViewBinding

class BasicNavigationViewActivity : AppCompatActivity() {

    lateinit var binding: ActivityBasicNavigationViewBinding
    lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBasicNavigationViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        mapboxMap.loadStyle("asset://map_style_blank.json")
    }
}
