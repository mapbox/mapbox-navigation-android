package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.content.Context
import android.view.MotionEvent
import com.mapbox.android.gestures.AndroidGesturesManager

internal class LocationGesturesManager(
    context: Context,
    private val onUpEventCallback: (AndroidGesturesManager) -> Unit,
    private val onDownEventCallback: (AndroidGesturesManager) -> Unit,
) : AndroidGesturesManager(context) {
    override fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        if (motionEvent != null) {
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_UP -> onUpEventCallback.invoke(this)
                MotionEvent.ACTION_DOWN -> onDownEventCallback.invoke(this)
            }
        }
        return super.onTouchEvent(motionEvent)
    }
}
