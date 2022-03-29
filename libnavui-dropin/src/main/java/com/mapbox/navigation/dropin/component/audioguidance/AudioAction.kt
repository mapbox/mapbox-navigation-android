package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.model.Action

/**
 * Defines actions responsible to mutate the [AudioGuidanceState].
 */
@ExperimentalPreviewMapboxNavigationAPI
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
