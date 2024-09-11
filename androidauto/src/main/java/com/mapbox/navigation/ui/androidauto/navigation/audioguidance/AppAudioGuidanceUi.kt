package com.mapbox.navigation.ui.androidauto.navigation.audioguidance

import androidx.annotation.UiThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.voice.api.MapboxAudioGuidanceState

/**
 * Use this function to mute the audio guidance for a lifecycle.
 */
@UiThread
fun Lifecycle.muteAudioGuidance() {
    addObserver(
        object : DefaultLifecycleObserver {
            lateinit var initialState: MapboxAudioGuidanceState
            override fun onResume(owner: LifecycleOwner) {
                with(MapboxAudioGuidance.getRegisteredInstance()) {
                    initialState = stateFlow().value
                    mute()
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                if (!initialState.isMuted) {
                    MapboxAudioGuidance.getRegisteredInstance().unmute()
                }
            }
        },
    )
}
