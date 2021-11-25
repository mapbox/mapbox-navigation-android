package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

sealed class SoundButtonAction {
    data class UpdateNavigationState(
        val navigationState: NavigationState
    ) : SoundButtonAction()

    data class UpdateVolume(
        val volume: Float
    ) : SoundButtonAction()
}

internal sealed class SoundButtonResult {
    data class OnNavigationState(
        val navigationState: NavigationState
    ) : SoundButtonResult()

    data class OnVisibility(
        val isVisible: Boolean
    ) : SoundButtonResult()

    data class OnVolume(
        val volume: Float,
        val isMute: Boolean
    ) : SoundButtonResult()
}
