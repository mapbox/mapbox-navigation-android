
package com.mapbox.navigation.examples.androidauto.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.examples.androidauto.CarAppSyncComponent
import com.mapbox.navigation.examples.androidauto.databinding.MapboxActivityNavigationViewBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MapboxActivityNavigationViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MapboxActivityNavigationViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO going to expose a public api to share a replay controller
        // This allows to simulate your location
//        binding.navigationView.api.routeReplayEnabled(true)

        CarAppSyncComponent.getInstance().attachNavigationView(binding.navigationView)
    }
}
