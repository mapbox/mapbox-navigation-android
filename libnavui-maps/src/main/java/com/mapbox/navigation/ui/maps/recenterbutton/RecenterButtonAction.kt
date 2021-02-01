package com.mapbox.navigation.ui.maps.recenterbutton

internal sealed class RecenterButtonAction {

    data class RecenterButtonVisibilityChanged(
        val isVisible: Boolean
    ): RecenterButtonAction()

    object OnRecenterButtonClicked: RecenterButtonAction()
}
