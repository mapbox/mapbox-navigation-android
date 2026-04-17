package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.content.Context
import android.view.MotionEvent
import com.mapbox.android.gestures.AndroidGesturesManager
import com.mapbox.android.gestures.BaseGesture

internal class OnUpEventDetector(
    context: Context,
    private val gesturesManager: AndroidGesturesManager,
    private val onUpEventCallback: (AndroidGesturesManager) -> Unit,
) : BaseGesture<Unit>(context, gesturesManager) {
    override fun analyzeEvent(motionEvent: MotionEvent): Boolean {
        val action: Int = motionEvent.actionMasked
        if (action == MotionEvent.ACTION_UP) {
            onUpEventCallback.invoke(gesturesManager)
        }
        return false
    }
}
