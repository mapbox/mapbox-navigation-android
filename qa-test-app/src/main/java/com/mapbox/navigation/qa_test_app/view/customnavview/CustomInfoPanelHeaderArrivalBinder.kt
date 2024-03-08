package com.mapbox.navigation.qa_test_app.view.customnavview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.qa_test_app.databinding.LayoutInfoPanelHeaderArrivalBinding
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

class CustomInfoPanelHeaderArrivalBinder(
    private val navigationView: NavigationView
): UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        viewGroup.removeAllViews()

        val inflater = LayoutInflater.from(viewGroup.context)
        val binding = LayoutInfoPanelHeaderArrivalBinding.inflate(inflater, viewGroup)

        return CustomInfoPanelHeaderArrivalComponent(navigationView, binding)
    }
}

class CustomInfoPanelHeaderArrivalComponent(
    private val navigationView: NavigationView,
    private val binding: LayoutInfoPanelHeaderArrivalBinding,
): UIComponent() {

    private var bottomSheetBehavior = navigationView.getBottomSheetBehaviour()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        binding.container.animate()
            .setDuration(500)
            .alpha(1f)
            .start()

        updateToggleButton(bottomSheetBehavior.state) // set button initial state
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        binding.toggleButton.setOnClickListener {
            bottomSheetBehavior.state =
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) BottomSheetBehavior.STATE_COLLAPSED
                else BottomSheetBehavior.STATE_EXPANDED
        }

        binding.endNavButton.setOnClickListener {
            navigationView.api.startFreeDrive()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
        binding.toggleButton.setOnClickListener(null)
        binding.endNavButton.setOnClickListener(null)
    }

    private fun updateToggleButton(bottomSheetState: Int) {
        binding.toggleButton.text =
            if (bottomSheetState == BottomSheetBehavior.STATE_EXPANDED) "Close"
            else "Open"
    }

    private val bottomSheetCallback = object: BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            updateToggleButton(newState)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }
}

private fun NavigationView.getBottomSheetBehaviour(): BottomSheetBehavior<ViewGroup> {
    val infoPanelLayout = findViewById<ViewGroup>(com.mapbox.navigation.dropin.R.id.infoPanelLayout)
    return BottomSheetBehavior.from(infoPanelLayout)
}
