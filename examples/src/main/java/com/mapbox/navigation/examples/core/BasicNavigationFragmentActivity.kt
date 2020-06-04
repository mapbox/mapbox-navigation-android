package com.mapbox.navigation.examples.core

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.examples.R

/**
 * This activity shows how to create a turn-by-turn navigation from a Fragment.
 */
class BasicNavigationFragmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_navigation_fragment)
    }
}
