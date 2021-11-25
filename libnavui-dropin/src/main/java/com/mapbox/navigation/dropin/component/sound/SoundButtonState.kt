package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

internal data class SoundButtonState(
    val isVisible: Boolean,
    val volume: Float,
    val isMute: Boolean,
    val navigationState: NavigationState
) {
    companion object {
        fun initial(): SoundButtonState = SoundButtonState(
            isVisible = false,
            volume = 0f,
            isMute = true,
            navigationState = NavigationState.Empty,
        )
    }
}
