package com.mapbox.navigation.examples.core

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.examples.core.databinding.LayoutActivityNavigationViewFragmentBinding

class MapboxNavigationViewFragmentActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityNavigationViewFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityNavigationViewFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
