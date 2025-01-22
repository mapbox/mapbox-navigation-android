package com.mapbox.navigation.voice.api

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechVolume
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Online implementation of [VoiceInstructionsPlayer].
 * Will retrieve synthesized speech mp3s from Mapbox's API Voice.
 * @property context Context
 * @property playerAttributes [VoiceInstructionsPlayerAttributes]
 */
internal class VoiceInstructionsFilePlayer(
    private val context: Context,
    private val playerAttributes: VoiceInstructionsPlayerAttributes,
) : VoiceInstructionsPlayer {

    @VisibleForTesting
    internal var mediaPlayer: MediaPlayer? = null

    @VisibleForTesting
    internal var volumeLevel: Float = DEFAULT_VOLUME_LEVEL
    private var clientCallback: VoiceInstructionsPlayerCallback? = null

    @VisibleForTesting
    internal var currentPlay: SpeechAnnouncement? = null

    /**
     * Given [SpeechAnnouncement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param callback
     */
    override fun play(
        announcement: SpeechAnnouncement,
        callback: VoiceInstructionsPlayerCallback,
    ) {
        clientCallback = callback
        check(currentPlay == null) {
            "Only one announcement can be played at a time."
        }
        currentPlay = announcement
        val file = announcement.file
        if (file != null && file.canRead()) {
            play(file)
        } else {
            logE(
                "Announcement file from state can't be null and needs to be accessible",
                LOG_CATEGORY,
            )
            donePlaying(mediaPlayer)
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechVolume].
     * @param state SpeechState Volume level.
     */
    override fun volume(state: SpeechVolume) {
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
            val currentFileInputStream = FileInputStreamProvider.retrieveFileInputStream(
                instruction,
            )
            currentFileInputStream.use { fis ->
                val currentMediaPlayer = MediaPlayerProvider.retrieveMediaPlayer()
                mediaPlayer = currentMediaPlayer!!.apply {
                    setDataSource(fis!!.fd)
                    playerAttributes.applyOn(this)
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
                logE(
                    "MediaPlayer error: $what - extra: $extra",
                    LOG_CATEGORY,
                )
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
        currentPlay?.let {
            currentPlay = null
            clientCallback?.onDone(it)
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

        private const val LOG_CATEGORY = "VoiceInstructionsFilePlayer"
        private const val DEFAULT_VOLUME_LEVEL = 1.0f
    }
}
