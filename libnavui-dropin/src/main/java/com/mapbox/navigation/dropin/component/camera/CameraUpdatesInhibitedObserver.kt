package com.mapbox.navigation.dropin.component.camera

internal fun interface CameraUpdatesInhibitedObserver {
    fun cameraUpdatesInhibitedStatus(status: Boolean)
}
