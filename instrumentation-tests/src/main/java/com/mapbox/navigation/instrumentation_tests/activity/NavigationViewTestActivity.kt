package com.mapbox.navigation.instrumentation_tests.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.instrumentation_tests.databinding.ActivityDropinLayoutBinding

class NavigationViewTestActivity : AppCompatActivity() {

    private val viewBinding: ActivityDropinLayoutBinding by lazy {
        ActivityDropinLayoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
    }
}
