package com.mapbox.navigation.dropin.component.backstack

import android.view.KeyEvent
import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class BackStackComponent(
    val view: View,
    val backPressManager: BackPressManager,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        view.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                backPressManager.handleOnBackPressed()
            } else {
                false
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        view.setOnKeyListener { _, _, _ -> false }
    }
}
