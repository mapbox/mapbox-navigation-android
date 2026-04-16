package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.content.Context
import com.mapbox.android.gestures.AndroidGesturesManager

internal object NavigationCameraLifecycleProvider {
    fun getCustomOnUpDetector(
        context: Context,
        gesturesManager: AndroidGesturesManager,
        onUpEventCallback: (AndroidGesturesManager) -> Unit,
    ): OnUpEventDetector = OnUpEventDetector(
        context = context,
        gesturesManager = gesturesManager,
        onUpEventCallback = onUpEventCallback,
    )
}
