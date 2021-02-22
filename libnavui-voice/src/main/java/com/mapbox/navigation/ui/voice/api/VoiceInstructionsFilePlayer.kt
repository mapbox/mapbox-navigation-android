package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.mapbox.navigation.ui.base.api.voice.VoiceInstructionsPlayer
import com.mapbox.navigation.ui.base.api.voice.VoiceInstructionsPlayerCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Online implementation of [VoiceInstructionsPlayer].
 * Will retrieve synthesized speech mp3s from Mapbox's API Voice.
 * @property context Context
 * @property accessToken String
 * @property language [Locale] language (ISO 639)
 */
internal class VoiceInstructionsFilePlayer(
    private val context: Context,
    private val accessToken: String,
    private val language: String
) : VoiceInstructionsPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var volumeLevel: Float = DEFAULT_VOLUME_LEVEL
    private var clientCallback: VoiceInstructionsPlayerCallback? = null
    private var currentPlay: SpeechState.ReadyToPlay? = null

    /**
     * Given [SpeechState.ReadyToPlay] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param callback
     */
    override fun play(state: SpeechState.ReadyToPlay, callback: VoiceInstructionsPlayerCallback) {
        clientCallback = callback
        check(currentPlay == null) {
            "Only one announcement can be played at a time."
        }
        currentPlay = state
        val file = state.announcement.file
        if (file != null && file.canRead()) {
            play(file)
        } else {
            Log.e(TAG, "Announcement file from state can't be null and needs to be accessible")
            donePlaying(mediaPlayer)
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechState.Volume].
     * @param state SpeechState Volume level.
     */
    override fun volume(state: SpeechState.Volume) {
        volumeLevel = state.level
        setVolume(volumeLevel)
    }

    /**
     * Clears any announcements queued.
     */
    override fun clear() {
        resetMediaPlayer(mediaPlayer)
        currentPlay = null
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        clear()
        volumeLevel = DEFAULT_VOLUME_LEVEL
    }

    private fun play(instruction: File) {
        try {
            FileInputStream(instruction).use { fis ->
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(fis.fd)
                    prepareAsync()
                }
                setVolume(volumeLevel)
                addListeners()
            }
        } catch (ex: FileNotFoundException) {
            donePlaying(mediaPlayer)
        } catch (ex: IOException) {
            donePlaying(mediaPlayer)
        }
    }

    private fun addListeners() {
        mediaPlayer?.run {
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: $what - extra: $extra")
                false
            }
            setOnPreparedListener { mp ->
                mp.start()
            }
            setOnCompletionListener { mp ->
                donePlaying(mp)
            }
        }
    }

    private fun donePlaying(mp: MediaPlayer?) {
        resetMediaPlayer(mp)
        currentPlay?.announcement?.let {
            currentPlay = null
            clientCallback?.onDone(SpeechState.DonePlaying(it))
        }
    }

    private fun setVolume(level: Float) {
        mediaPlayer?.setVolume(level, level)
    }

    private fun resetMediaPlayer(mp: MediaPlayer?) {
        mp?.release()
        mediaPlayer = null
    }

    private companion object {
        private const val TAG = "MbxVoiceInstructionsFilePlayer"
        private const val DEFAULT_VOLUME_LEVEL = 0.5f
    }
}
