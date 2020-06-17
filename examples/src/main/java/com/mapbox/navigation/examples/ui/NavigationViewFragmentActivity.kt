package com.mapbox.navigation.examples.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.examples.R

class NavigationViewFragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.navigation_view_fragment_activity_layout)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.rootLayout, NavigationViewFragment.newInstance(),
                    "navigationViewFragment"
                )
                .commit()
        }
    }

    override fun onBackPressed() {
        val fragment =
            (
                supportFragmentManager.findFragmentByTag("navigationViewFragment")
                    as NavigationViewFragment
                )
        if (!fragment.navigationViewBackPressed()) {
            super.onBackPressed()
        }
    }
}
