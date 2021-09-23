package com.mapbox.navigation.examples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.examples.core.databinding.LayoutActivityDropInBinding

class DropInActivity: AppCompatActivity() {

    private lateinit var binding: LayoutActivityDropInBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityDropInBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
