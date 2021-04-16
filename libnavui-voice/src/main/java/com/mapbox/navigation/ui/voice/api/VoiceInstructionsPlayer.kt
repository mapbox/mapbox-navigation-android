package com.mapbox.navigation.ui.voice.api

import android.media.AudioManager
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
     * The method will set the volume to the specified level from [SpeechVolume].
     * @param state volume level.
     */
    fun volume(state: SpeechVolume)

    /**
     * The method will set the audio stream type for player.
     * @param type Audio stream type. See [AudioManager] for a list of stream types.
     */
    fun stream(type: Int)

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
