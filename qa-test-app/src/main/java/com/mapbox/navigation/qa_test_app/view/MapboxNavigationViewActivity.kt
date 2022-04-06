package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewBinding

class MapboxNavigationViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
