package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewFragmentBinding

class MapboxNavigationViewFragmentActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityNavigationViewFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityNavigationViewFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
