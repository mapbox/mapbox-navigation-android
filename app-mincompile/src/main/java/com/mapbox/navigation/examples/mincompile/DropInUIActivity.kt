package com.mapbox.navigation.examples.mincompile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mapbox.navigation.examples.mincompile.databinding.LayoutActivityDropinBinding

class DropInUIActivity : AppCompatActivity() {
    private lateinit var binding: LayoutActivityDropinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_main)
        binding = LayoutActivityDropinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This allows to simulate your location
        binding.navigationView.api.routeReplayEnabled(true)
    }
}
