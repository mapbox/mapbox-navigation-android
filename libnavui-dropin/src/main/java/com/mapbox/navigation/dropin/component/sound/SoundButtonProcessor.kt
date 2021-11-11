package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

internal sealed interface SoundButtonProcessor {

    fun process(): SoundButtonResult

    data class ProcessNavigationState(val navigationState: NavigationState) : SoundButtonProcessor {
        override fun process(): SoundButtonResult.OnNavigationState =
            SoundButtonResult.OnNavigationState(
                navigationState = navigationState
            )
    }

    data class ProcessVisibility(val navigationState: NavigationState) : SoundButtonProcessor {
        private val visibilitySet = setOf(
            NavigationState.ActiveNavigation,
            NavigationState.Arrival
        )
        override fun process(): SoundButtonResult.OnVisibility =
            SoundButtonResult.OnVisibility(
                isVisible = visibilitySet.contains(navigationState)
            )
    }

    data class ProcessVolumeUpdates(
        val volume: Float
    ) : SoundButtonProcessor {
        override fun process(): SoundButtonResult.OnVolume {
            val isMute = volume == 0f
            return SoundButtonResult.OnVolume(volume = volume, isMute = isMute)
        }
    }
}
