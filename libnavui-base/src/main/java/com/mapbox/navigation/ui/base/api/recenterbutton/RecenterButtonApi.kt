package com.mapbox.navigation.ui.base.api.recenterbutton

interface RecenterButtonApi {

    fun recenterButtonVisible(isVisible: Boolean, callback: RecenterButtonCallback)

    fun onRecenterButtonClicked(callback: RecenterButtonCallback)
}
