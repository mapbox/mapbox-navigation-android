package com.mapbox.navigation.examples.core

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.MapboxNavigationViewApi
import com.mapbox.navigation.dropin.ViewProvider
import com.mapbox.navigation.examples.core.databinding.LayoutActivityNavigationViewBinding

class MapboxNavigationViewActivity : AppCompatActivity() {

    private lateinit var mapboxNavigationViewApi: MapboxNavigationViewApi

    private lateinit var binding: LayoutActivityNavigationViewBinding

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
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
