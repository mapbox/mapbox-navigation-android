package com.mapbox.navigation.examples.core

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.examples.core.databinding.LayoutActivityNavigationViewBinding

class MapboxNavigationViewActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityNavigationViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
