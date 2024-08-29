package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.content.Context
import android.view.MotionEvent
import com.mapbox.android.gestures.AndroidGesturesManager

internal class LocationGesturesManager(
    context: Context,
    private val onUpEventCallback: (AndroidGesturesManager) -> Unit,
) : AndroidGesturesManager(context) {
    override fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        if (motionEvent != null) {
            val action: Int = motionEvent.actionMasked
            if (action == MotionEvent.ACTION_UP) {
                onUpEventCallback.invoke(this)
            }
        }
        return super.onTouchEvent(motionEvent)
    }
}
