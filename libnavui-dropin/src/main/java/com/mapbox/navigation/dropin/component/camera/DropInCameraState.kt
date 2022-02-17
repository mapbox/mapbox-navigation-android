package com.mapbox.navigation.dropin.component.camera

import androidx.lifecycle.MutableLiveData
import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInCameraState {
    val cameraMode = MutableLiveData(defaultCameraMode)
    val cameraOptions = MutableLiveData(defaultCameraOptions)
    var triggerIdleCameraOnMoveListener: Boolean = true

    private val _cameraUpdateEvent = MutableSharedFlow<CameraUpdateEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val cameraUpdateEvent = _cameraUpdateEvent.asSharedFlow()

    fun requestCameraUpdate(event: CameraUpdateEvent) {
        _cameraUpdateEvent.tryEmit(event)
    }

    fun cameraMode() = cameraMode.value ?: defaultCameraMode

    companion object {
        val defaultCameraMode: DropInCameraMode = DropInCameraMode.FOLLOWING

        private const val DEFAULT_INITIAL_ZOOM = 15.0
        private val defaultCameraOptions = CameraOptions.Builder()
            .zoom(DEFAULT_INITIAL_ZOOM)
    }

    abstract class CameraUpdateEvent(
        val cameraOptions: CameraOptions
    ) {
        class EaseTo(options: CameraOptions) : CameraUpdateEvent(options)
        class FlyTo(options: CameraOptions) : CameraUpdateEvent(options)
        class SetTo(options: CameraOptions) : CameraUpdateEvent(options)
    }
}
