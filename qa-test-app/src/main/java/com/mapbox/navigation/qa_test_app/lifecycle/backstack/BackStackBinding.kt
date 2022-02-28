package com.mapbox.navigation.qa_test_app.lifecycle.backstack

import android.view.KeyEvent
import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.utils.internal.logE

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class BackStackBinding(val view: View) : MapboxNavigationObserver {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                logE(TAG, "TODO handle back pressed")
                false
            } else {
                false
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        view.isFocusableInTouchMode = false
        view.setOnKeyListener { _, _, _ -> false }
    }

    private companion object {
        private const val TAG = "MbxNavigationBackStack"
    }
}
