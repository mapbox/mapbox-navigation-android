package com.mapbox.navigation.ui.voice.api

import android.content.Context
import androidx.annotation.UiThread
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.utils.internal.Provider
import com.mapbox.navigation.ui.voice.api.AudioFocusDelegateProvider.defaultAudioFocusDelegate
import com.mapbox.navigation.ui.voice.model.AudioFocusOwner
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import java.util.Locale
import java.util.Queue
import java.util.Timer
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.schedule

/**
 * Hybrid implementation of [MapboxVoiceInstructionsPlayer] combining [VoiceInstructionsTextPlayer] and
 * [VoiceInstructionsFilePlayer] speech players.
 * @property context Context
 * @property accessToken String
 * @property language [Locale] language (ISO 639)
 * @property options [VoiceInstructionsPlayerOptions] (optional)
 * @property audioFocusDelegate [AsyncAudioFocusDelegate] (optional)
 * @property timerFactory [Provider] (optional)
 */
@UiThread
class MapboxVoiceInstructionsPlayer @JvmOverloads constructor(
    private val context: Context,
    private val accessToken: String,
    private val language: String,
    private val options: VoiceInstructionsPlayerOptions = defaultOptions(),
    private val audioFocusDelegate: AsyncAudioFocusDelegate =
        defaultAudioFocusDelegate(context, options),
    private var timerFactory: Provider<Timer> = defaultTimerFactory()
) {
    constructor(
        context: Context,
        accessToken: String,
        language: String,
        options: VoiceInstructionsPlayerOptions = defaultOptions(),
        audioFocusDelegate: AudioFocusDelegate,
    ) : this(context, accessToken, language, options, wrapDelegate(audioFocusDelegate))

    private val attributes: VoiceInstructionsPlayerAttributes =
        VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(options)
    private val playCallbackQueue: Queue<PlayCallback> = ConcurrentLinkedQueue()
    private val filePlayer: VoiceInstructionsFilePlayer =
        VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
            context,
            accessToken,
            attributes,
        )
    private val textPlayer: VoiceInstructionsTextPlayer =
        VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
            context,
            language,
            attributes,
        )

    private var abandonFocusTimer: Timer? = null

    private val doneCallback: VoiceInstructionsPlayerCallback =
        VoiceInstructionsPlayerCallback {
            val currentPlayCallback = playCallbackQueue.poll()
            if (playCallbackQueue.isEmpty()) {
                abandonFocus()
            }

            val currentAnnouncement = currentPlayCallback.announcement
            val currentClientCallback = currentPlayCallback.consumer
            currentClientCallback.accept(currentAnnouncement)
            play()
        }

    /**
     * Given [SpeechAnnouncement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param consumer represents that the speech player is done playing
     */
    fun play(
        announcement: SpeechAnnouncement,
        consumer: MapboxNavigationConsumer<SpeechAnnouncement>
    ) {
        playCallbackQueue.add(PlayCallback(announcement, consumer))
        if (playCallbackQueue.size == 1) {
            play()
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechVolume].
     * Volume is specified as a float ranging from 0 to 1
     * where 0 is silence, and 1 is the maximum volume (the default behavior).
     *
     * Throws an [IllegalArgumentException] if the [SpeechVolume.level] is not in 0..1 range.
     *
     * @param state volume level.
     */
    @Throws(IllegalArgumentException::class)
    fun volume(state: SpeechVolume) {
        require(state.level in 0.0f..1.0f) { "Volume must be in 0..1 range." }
        filePlayer.volume(state)
        textPlayer.volume(state)
    }

    /**
     * Clears any announcements queued.
     */
    fun clear() {
        finalize()
        filePlayer.clear()
        textPlayer.clear()
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    fun shutdown() {
        finalize()
        filePlayer.shutdown()
        textPlayer.shutdown()
    }

    private fun play() {
        val currentPlayCallback = playCallbackQueue.peek() ?: return

        val currentPlay = currentPlayCallback.announcement
        val owner = when (currentPlay.file) {
            null -> AudioFocusOwner.TextToSpeech
            else -> AudioFocusOwner.MediaPlayer
        }

        requestFocus(owner) { isGranted ->
            if (isGranted) {
                when (owner) {
                    AudioFocusOwner.MediaPlayer -> filePlayer.play(currentPlay, doneCallback)
                    AudioFocusOwner.TextToSpeech -> textPlayer.play(currentPlay, doneCallback)
                }
            } else {
                doneCallback.onDone(currentPlay)
            }
        }
    }

    private fun finalize() {
        playCallbackQueue.clear()
        abandonFocus(true)
    }

    private fun requestFocus(owner: AudioFocusOwner, callback: AudioFocusRequestCallback) {
        abandonFocusTimer?.cancel()
        audioFocusDelegate.requestFocus(owner, callback)
    }

    private fun abandonFocus(immediate: Boolean = false) {
        abandonFocusTimer?.cancel()
        if (immediate) {
            audioFocusDelegate.abandonFocus()
        } else {
            abandonFocusTimer = timerFactory.get().apply {
                schedule(options.abandonFocusDelay) {
                    audioFocusDelegate.abandonFocus()
                }
            }
        }
    }

    private fun AsyncAudioFocusDelegate.abandonFocus() = abandonFocus { /* no-op */ }

    private companion object {

        private fun wrapDelegate(delegate: AudioFocusDelegate): AsyncAudioFocusDelegate {
            return object : AsyncAudioFocusDelegate {
                override fun requestFocus(
                    owner: AudioFocusOwner,
                    callback: AudioFocusRequestCallback
                ) {
                    callback(delegate.requestFocus())
                }

                override fun abandonFocus(callback: AudioFocusRequestCallback) {
                    callback(delegate.abandonFocus())
                }
            }
        }

        private fun defaultOptions() = VoiceInstructionsPlayerOptions.Builder().build()

        private fun defaultTimerFactory() = Provider { Timer() }
    }
}
