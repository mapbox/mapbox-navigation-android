package com.mapbox.navigation.ui.base.api.recenterbutton

import com.mapbox.navigation.ui.base.model.recenterbutton.RecenterButtonState

interface RecenterButtonCallback {

    fun onRecenterButtonVisibilityChanged(visibility: RecenterButtonState.RecenterButtonVisible)

    fun onRecenterButtonClicked(clickedState: RecenterButtonState.RecenterButtonClicked)
}
