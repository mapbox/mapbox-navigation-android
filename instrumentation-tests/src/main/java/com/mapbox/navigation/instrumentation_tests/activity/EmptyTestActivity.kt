package com.mapbox.navigation.instrumentation_tests.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.instrumentation_tests.R

class EmptyTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty_test)
    }
}
