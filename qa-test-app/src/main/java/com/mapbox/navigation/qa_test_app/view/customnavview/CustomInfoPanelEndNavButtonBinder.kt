package com.mapbox.navigation.qa_test_app.view.customnavview

import android.view.Gravity
import android.view.ViewGroup
import androidx.core.view.setPadding
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewApi
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CustomInfoPanelEndNavButtonBinder(
    private val api: NavigationViewApi
) : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val button = MapboxExtendableButton(
            viewGroup.context,
            null,
            R.style.DropInStyleExitButton
        )
        button.id = R.id.end_nav_button
        button.iconImage.setImageResource(R.drawable.mapbox_ic_stop_navigation)
        button.setPadding(0)
        button.setBackgroundResource(R.drawable.mapbox_bg_circle_outline)
        button.foregroundGravity = Gravity.CENTER
        val lp = ViewGroup.MarginLayoutParams(52.dp, 52.dp).apply {
            marginEnd = 30.dp
        }
        viewGroup.removeAllViews()
        viewGroup.addView(button, lp)

        return object : UIComponent() {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                super.onAttached(mapboxNavigation)
                button.setOnClickListener {
                    api.startFreeDrive()
                }
            }
        }
    }
}
