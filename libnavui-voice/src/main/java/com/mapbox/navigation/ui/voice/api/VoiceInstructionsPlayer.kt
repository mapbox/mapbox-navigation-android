package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume

/**
 * An Api that allows you to interact with the speech player
 */
internal interface VoiceInstructionsPlayer {

    /**
     * Given [SpeechAnnouncement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param callback
     */
    fun play(announcement: SpeechAnnouncement, callback: VoiceInstructionsPlayerCallback)

    /**
     * Cancels playing the provided [SpeechAnnouncement].
     * It may be useful for cases when playing the instruction is useless after some time.
     * @param announcement the instruction to be cancelled
     */
    fun cancel(announcement: SpeechAnnouncement)

    /**
     * The method will set the volume to the specified level from [SpeechVolume].
     * @param state volume level.
     */
    fun volume(state: SpeechVolume)

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
