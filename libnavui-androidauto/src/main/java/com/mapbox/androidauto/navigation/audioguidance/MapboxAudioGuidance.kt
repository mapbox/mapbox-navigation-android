package com.mapbox.androidauto.navigation.audioguidance

import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import kotlinx.coroutines.flow.StateFlow

/**
 * Service interface accessible through [MapboxCarApp.carAppServices]
 *
 * It will start monitoring audio guidance once the class is referenced because it is lazy loaded.
 * Use the controllable functions [mute], [unmute], [toggle] to change the voice guidance. The
 * selection is saved to shared preferences and will become the default setting.
 *
 * Subscribing onto the [stateFlow] does not change the internal state.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
interface MapboxAudioGuidance : MapboxNavigationObserver {
    /**
     * Monitor the voice instructions state.
     */
    fun stateFlow(): StateFlow<State>

    /**
     * Continue to monitor voice instructions, but stop the audio voice.
     */
    fun mute()

    /**
     * Turn on the audio voice.
     */
    fun unmute()

    /**
     * Toggle the state between [mute] and [unmute].
     */
    fun toggle()

    /**
     * Accessed through [stateFlow].
     */
    interface State {
        /**
         * When the trip session has started, and there is an active route.
         */
        val isPlayable: Boolean

        /**
         * Unrelated to the navigation state, provides state of
         * the controls [mute] [unmute] [toggle]
         */
        val isMuted: Boolean

        /**
         * Once a voice instruction becomes available this will not be null.
         * When the state [isPlayable] and this is null, it means there are no voice instructions
         * on the route at this time.
         */
        val voiceInstructions: VoiceInstructions?

        /**
         * After a [voiceInstructions] has been announced, this value will be emitted.
         * This will always be null when [isMuted] is true.
         */
        val speechAnnouncement: SpeechAnnouncement?
    }
}
