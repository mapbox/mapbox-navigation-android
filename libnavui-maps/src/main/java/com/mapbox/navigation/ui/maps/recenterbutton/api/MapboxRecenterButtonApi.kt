package com.mapbox.navigation.ui.maps.recenterbutton.api

import com.mapbox.navigation.ui.base.api.recenterbutton.RecenterButtonApi
import com.mapbox.navigation.ui.base.api.recenterbutton.RecenterButtonCallback
import com.mapbox.navigation.ui.base.model.recenterbutton.RecenterButtonState
import com.mapbox.navigation.ui.maps.recenterbutton.RecenterButtonAction
import com.mapbox.navigation.ui.maps.recenterbutton.RecenterButtonProcessor
import com.mapbox.navigation.ui.maps.recenterbutton.RecenterButtonResult

class MapboxRecenterButtonApi: RecenterButtonApi {

    override fun recenterButtonVisible(isVisible: Boolean, callback: RecenterButtonCallback) {
        val result = RecenterButtonProcessor.process(
            RecenterButtonAction.RecenterButtonVisibilityChanged(isVisible)
        )
        when (result) {
            is RecenterButtonResult.RecenterButtonVisible -> {
                callback.onRecenterButtonVisibilityChanged(
                    RecenterButtonState.RecenterButtonVisible(true)
                )
            }
            is RecenterButtonResult.RecenterButtonInvisible -> {
                callback.onRecenterButtonVisibilityChanged(
                    RecenterButtonState.RecenterButtonVisible(true)
                )
            }
        }
    }

    override fun onRecenterButtonClicked(callback: RecenterButtonCallback) {
        val result = RecenterButtonProcessor.process(
            RecenterButtonAction.OnRecenterButtonClicked
        )
        if (result is RecenterButtonResult.OnRecenterButtonClicked) {
            callback.onRecenterButtonClicked(
                RecenterButtonState.RecenterButtonClicked
            )
        }
    }

}
