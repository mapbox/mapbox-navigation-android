package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState

/**
 * An Api that allows you to interact with the speech player
 */
interface VoiceInstructionsPlayer {

    /**
     * Given [SpeechState.ReadyToPlay] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param callback
     */
    fun play(state: SpeechState.ReadyToPlay, callback: VoiceInstructionsPlayerCallback)

    /**
     * The method will set the volume to the specified level from [SpeechState.Volume].
     * @param state SpeechState Volume level.
     */
    fun volume(state: SpeechState.Volume)

    /**
     * Clears any announcements queued.
     */
    fun clear()

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    fun shutdown()
}
