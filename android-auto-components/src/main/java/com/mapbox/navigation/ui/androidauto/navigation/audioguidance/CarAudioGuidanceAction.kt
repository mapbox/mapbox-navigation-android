package com.mapbox.navigation.ui.androidauto.navigation.audioguidance

import androidx.annotation.DrawableRes
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.WeakHashMap

/**
 * This class creates an action that can control audio guidance.
 */
class CarAudioGuidanceAction {

    /**
     * Build the [Action].
     */
    fun getAction(screen: Screen): Action {
        screen.invalidateOnStateChange()
        return buildSoundButtonAction(screen)
    }

    // The action is rebuilt with every template, so each screen starts its observer only once.
    // The observer lives in the screen's lifecycle scope: it collects only while the screen is
    // started and ends when the screen is destroyed. The host requests a new template when a
    // screen is started again, which picks up a state changed while it was stopped.
    private fun Screen.invalidateOnStateChange() {
        if (!observedScreens.add(this)) return
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MapboxAudioGuidance.getRegisteredInstance().stateFlow()
                    .distinctUntilChanged { old, new ->
                        old.isMuted == new.isMuted && old.isPlayable == new.isPlayable
                    }
                    .drop(1)
                    .collect { invalidate() }
            }
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
        // Weak keys, so a destroyed screen is never kept in memory. Only used on the main thread.
        private val observedScreens: MutableSet<Screen> =
            Collections.newSetFromMap(WeakHashMap())
    }
}
