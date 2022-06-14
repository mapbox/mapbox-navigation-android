package com.mapbox.navigation.ui.app.internal.audioguidance

import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible to mutate the [AudioGuidanceState].
 */
sealed class AudioAction : Action {
    /**
     * The action mutes the volume control for audio guidance
     */
    object Mute : AudioAction()

    /**
     * The action un-mutes the volume control for audio guidance
     */
    object Unmute : AudioAction()

    /**
     * The action toggles mute/un-mute volume control for audio guidance
     */
    object Toggle : AudioAction()
}
