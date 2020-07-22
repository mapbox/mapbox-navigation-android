package com.mapbox.navigation.ui

import com.mapbox.navigation.ui.instruction.GuidanceViewListener

/**
 * Internal callback for NavigationView to handle Guidance view visibility changing event.
 */
internal class NavigationGuidanceViewListener(private val navigationPresenter: NavigationPresenter) :
        GuidanceViewListener {
    override fun onShownAt(left: Int, top: Int, width: Int, height: Int) {
        navigationPresenter.onGuidanceViewChange(left, top, width, height)
    }

    override fun onHidden() {
        navigationPresenter.onGuidanceViewChange(0, 0, 0, 0)
    }
}
