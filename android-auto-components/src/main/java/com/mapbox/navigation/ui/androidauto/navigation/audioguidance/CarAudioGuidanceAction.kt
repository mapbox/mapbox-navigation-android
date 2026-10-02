package com.mapbox.navigation.ui.androidauto.navigation.audioguidance

import androidx.annotation.DrawableRes
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.coroutineScope
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * This class creates an action that can control audio guidance.
 */
class CarAudioGuidanceAction {

    /**
     * Build the [Action].
     */
    fun getAction(screen: Screen): Action {
        screen.invalidateOnceAfterStateChange()
        return buildSoundButtonAction(screen)
    }

    // Actions are built but they are not destroyed, so the only way to know if an action is
    // destroyed is if the Screen host is destroyed. For the AudioGuidanceAction, we also assume
    // that there only needs to be one state change listener at a time.
    private fun Screen.invalidateOnceAfterStateChange() {
        invalidatorJob?.cancel()
        invalidatorJob = lifecycle.coroutineScope.launch {
            MapboxAudioGuidance.getRegisteredInstance().stateFlow()
                .distinctUntilChanged { old, new ->
                    old.isMuted == new.isMuted && old.isPlayable == new.isPlayable
                }
                .drop(1)
                .take(1)
                .collect { invalidate() }
        }
    }

    private fun buildSoundButtonAction(screen: Screen): Action {
        val audioGuidance = MapboxAudioGuidance.getRegisteredInstance()
        val state = audioGuidance.stateFlow().value
        return if (!state.isMuted) {
            buildIconAction(screen, R.drawable.mapbox_car_ic_volume_on) {
                audioGuidance.mute()
            }
        } else {
            buildIconAction(screen, R.drawable.mapbox_car_ic_volume_off) {
                audioGuidance.unmute()
            }
        }
    }

    private fun buildIconAction(
        screen: Screen,
        @DrawableRes icon: Int,
        onClick: () -> Unit,
    ) = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(screen.carContext, icon),
            ).build(),
        )
        .setOnClickListener { onClick() }
        .build()

    private companion object {
        private var invalidatorJob: Job? = null
    }
}
