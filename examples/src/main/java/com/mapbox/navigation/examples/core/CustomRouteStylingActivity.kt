package com.mapbox.navigation.examples.core

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_basic_navigation_layout.*

/**
 * Note: See the AndroidManifest.xml for this activity and notice how the theme is set.
 */
class CustomRouteStylingActivity : BasicNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fabToggleStyle.visibility = View.GONE
    }
}
