package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewBinding

class MapboxNavigationViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, binding.root.getOnBackPressedCallback())

        /**
         * This activity is demonstrating default behavior. Do not add anything to this class.
         *
         * Customized behavior can be demonstrated in [MapboxNavigationViewCustomizedActivity]
         */
    }
}
