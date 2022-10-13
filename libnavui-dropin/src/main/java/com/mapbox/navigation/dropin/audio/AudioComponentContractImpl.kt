package com.mapbox.navigation.dropin.audio

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.audioguidance.AudioAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.voice.internal.ui.AudioComponentContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class AudioComponentContractImpl(
    scope: CoroutineScope,
    val store: Store
) : AudioComponentContract {

    override val isMuted: StateFlow<Boolean> =
        store.slice(scope) { it.audio.isMuted }

    override val isVisible: StateFlow<Boolean> =
        store.slice(scope) { it.navigation == NavigationState.ActiveNavigation }

    override fun mute() {
        store.dispatch(AudioAction.Mute)
    }

    override fun unMute() {
        store.dispatch(AudioAction.Unmute)
    }
}
