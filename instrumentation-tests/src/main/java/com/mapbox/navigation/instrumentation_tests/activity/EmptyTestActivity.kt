package com.mapbox.navigation.instrumentation_tests.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.instrumentation_tests.databinding.ActivityEmptyTestBinding

class EmptyTestActivity : AppCompatActivity() {

    lateinit var binding: ActivityEmptyTestBinding
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmptyTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
