package com.mapbox.navigation.examples.core

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.dropin.NavigationViewInitializedObserver
import com.mapbox.navigation.dropin.ViewProvider
import com.mapbox.navigation.examples.core.databinding.LayoutActivityDropInBinding

class MapboxDropInActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityDropInBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityDropInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navigationView.navigationViewApi.configureNavigationView(ViewProvider())
    }
}
