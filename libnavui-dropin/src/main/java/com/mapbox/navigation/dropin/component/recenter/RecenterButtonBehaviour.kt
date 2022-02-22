package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.dropin.component.camera.DropInCameraState
import com.mapbox.navigation.dropin.component.camera.DropInCameraState.CameraUpdateEvent.EaseTo
import com.mapbox.navigation.dropin.component.location.LocationBehavior
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.toPoint

internal class RecenterButtonBehaviour(
    private val cameraState: DropInCameraState,
    private val locationBehavior: LocationBehavior,
) : UIComponent() {

    fun onButtonClick() {
        locationBehavior.locationLiveData.value?.also {
            val cameraOptions = CameraOptions.Builder()
                .center(it.toPoint())
                .build()
            cameraState.requestCameraUpdate(EaseTo(cameraOptions))
        }
    }
}
