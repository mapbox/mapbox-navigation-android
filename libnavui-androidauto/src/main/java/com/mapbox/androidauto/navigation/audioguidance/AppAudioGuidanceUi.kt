package com.mapbox.androidauto.navigation.audioguidance

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun Fragment.attachAudioGuidance(
    mapboxSoundButton: MapboxSoundButton
) {
    val lifecycleOwner = viewLifecycleOwner
    val flow = MapboxCarApp.carAppAudioGuidanceService().stateFlow()
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { state ->
                if (state.isMuted) {
                    mapboxSoundButton.mute()
                } else {
                    mapboxSoundButton.unmute()
                }
                mapboxSoundButton.visibility = if (state.isPlayable) View.VISIBLE else View.GONE
            }
        }
    }
    mapboxSoundButton.setOnClickListener {
        MapboxCarApp.carAppAudioGuidanceService().toggle()
    }
}

/**
 * Use this function to mute the audio guidance for a lifecycle.
 */
fun Lifecycle.muteAudioGuidance() {
    addObserver(object : DefaultLifecycleObserver {
        lateinit var initialState: MapboxAudioGuidance.State
        override fun onResume(owner: LifecycleOwner) {
            with(MapboxCarApp.carAppAudioGuidanceService()) {
                initialState = stateFlow().value
                mute()
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            if (!initialState.isMuted) {
                MapboxCarApp.carAppAudioGuidanceService().unmute()
            }
        }
    })
}
