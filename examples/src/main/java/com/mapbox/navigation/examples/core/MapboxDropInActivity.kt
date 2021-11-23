package com.mapbox.navigation.examples.core

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.dropin.MapboxNavigationViewApi
import com.mapbox.navigation.dropin.ViewProvider
import com.mapbox.navigation.examples.core.databinding.LayoutActivityDropInBinding

class MapboxDropInActivity : AppCompatActivity() {

    private lateinit var mapboxNavigationViewApi: MapboxNavigationViewApi

    private lateinit var binding: LayoutActivityDropInBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityDropInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapboxNavigationViewApi = binding.navigationView.navigationViewApi
        mapboxNavigationViewApi.getOptions()
            .toBuilder(this)
            .useReplayEngine(true)
            .build().apply {
                mapboxNavigationViewApi.update(this)
            }
        mapboxNavigationViewApi.configureNavigationView(ViewProvider())

        binding.tempStartNavigation.setOnClickListener {
            mapboxNavigationViewApi.temporaryStartNavigation()
        }
    }
}
