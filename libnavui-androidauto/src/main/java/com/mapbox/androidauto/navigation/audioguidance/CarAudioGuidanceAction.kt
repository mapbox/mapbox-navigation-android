package com.mapbox.androidauto.navigation.audioguidance

import androidx.annotation.DrawableRes
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.androidauto.R
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidance
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * This class creates an action that can control audio guidance.
 */
class CarAudioGuidanceAction {

    /**
     * Build the [Action].
     */
    fun getAction(screen: Screen): Action {
        screen.lifecycle.apply {
            coroutineScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    MapboxAudioGuidance.getRegisteredInstance().stateFlow()
                        .distinctUntilChanged { old, new ->
                            old.isMuted == new.isMuted && old.isPlayable == new.isPlayable
                        }
                        .collect { screen.invalidate() }
                }
            }
        }

        return buildSoundButtonAction(screen)
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
        onClick: () -> Unit
    ) = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(screen.carContext, icon)
            ).build()
        )
        .setOnClickListener { onClick() }
        .build()
}
