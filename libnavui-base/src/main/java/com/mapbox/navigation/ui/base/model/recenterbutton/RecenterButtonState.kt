package com.mapbox.navigation.ui.base.model.recenterbutton

import com.mapbox.navigation.ui.base.MapboxState

sealed class RecenterButtonState: MapboxState {

    data class RecenterButtonVisible(val isVisible: Boolean): RecenterButtonState()

    object RecenterButtonClicked: RecenterButtonState()
}
