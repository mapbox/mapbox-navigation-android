package com.mapbox.navigation.dropin.component.recenter

import androidx.lifecycle.asFlow
import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.camera.DropInCameraMode
import com.mapbox.navigation.dropin.component.camera.DropInCameraState
import com.mapbox.navigation.dropin.component.camera.DropInCameraState.CameraUpdateEvent.EaseTo
import com.mapbox.navigation.dropin.component.location.LocationBehavior
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RecenterButtonBehaviour(
    private val cameraState: DropInCameraState,
    private val locationBehavior: LocationBehavior,
    private val shouldShowButton: VisibilityPolicy = VisibilityPolicy.WHEN_NOT_FOLLOWING
) : UIComponent() {

    private val _isButtonVisible = MutableStateFlow(false)
    val isButtonVisible = _isButtonVisible.asStateFlow()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        cameraState.cameraMode.asFlow().observe {
            _isButtonVisible.value = shouldShowButton(it)
        }
    }

    fun onButtonClick() {
        locationBehavior.locationLiveData.value?.also {
            val cameraOptions = CameraOptions.Builder()
                .center(it.toPoint())
                .build()
            cameraState.requestCameraUpdate(EaseTo(cameraOptions))
        }
    }

    enum class VisibilityPolicy(
        private val predicate: (cameraMode: DropInCameraMode) -> Boolean
    ) {
        WHEN_NOT_FOLLOWING({ it != DropInCameraMode.FOLLOWING });

        operator fun invoke(cameraMode: DropInCameraMode) = predicate(cameraMode)
    }
}
