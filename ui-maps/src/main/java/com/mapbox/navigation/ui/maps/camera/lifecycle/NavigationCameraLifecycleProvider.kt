package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.content.Context
import com.mapbox.android.gestures.AndroidGesturesManager

internal object NavigationCameraLifecycleProvider {
    fun getCustomGesturesManager(
        context: Context,
        onUpEventCallback: (AndroidGesturesManager) -> Unit,
    ): AndroidGesturesManager = LocationGesturesManager(context, onUpEventCallback)
}
