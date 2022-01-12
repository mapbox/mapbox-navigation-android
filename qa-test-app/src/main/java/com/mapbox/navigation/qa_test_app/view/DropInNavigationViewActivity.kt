package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.qa_test_app.databinding.DropinNavigationViewActivityLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInNavigationViewActivity : AppCompatActivity() {

    private val binding: DropinNavigationViewActivityLayoutBinding by lazy {
        DropinNavigationViewActivityLayoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        /**
         * Attach [MapboxNavigationApp] to an Activity or Fragment to be able to
         * handle orientation changes.
         */
        MapboxNavigationApp.attach(this)
    }
}
