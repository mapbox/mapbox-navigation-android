package com.mapbox.navigation.ui.voice.api

import android.content.Context
import androidx.annotation.UiThread
import com.mapbox.navigation.core.trip.session.VOICE_INSTRUCTION_LOG
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.utils.internal.Provider
import com.mapbox.navigation.ui.voice.api.AudioFocusDelegateProvider.defaultAudioFocusDelegate
import com.mapbox.navigation.ui.voice.model.AudioFocusOwner
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import com.mapbox.navigation.utils.internal.logI
import java.util.Locale
import java.util.Queue
import java.util.Timer
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.schedule

/**
 * Hybrid implementation of [MapboxVoiceInstructionsPlayer] combining [VoiceInstructionsTextPlayer] and
 * [VoiceInstructionsFilePlayer] speech players.
 * NOTE: do not use lazy initialization for this class since it takes some time to initialize
 * the system services required for on-device speech synthesis. With lazy initialization
 * there is a high risk that said services will not be available when the first instruction
 * has to be played. [MapboxVoiceInstructionsPlayer] should be instantiated in
 * `Activity#onCreate`.
 *
 * @param context Context
 * @param language [Locale] language (in a format acceptable by [Locale])
 * @param options [VoiceInstructionsPlayerOptions] (optional)
 * @param audioFocusDelegate [AsyncAudioFocusDelegate] (optional)
 * @param timerFactory [Provider] (optional)
 */
@UiThread
class MapboxVoiceInstructionsPlayer @JvmOverloads constructor(
    private val context: Context,
    language: String,
    private val options: VoiceInstructionsPlayerOptions = defaultOptions(),
    private val audioFocusDelegate: AsyncAudioFocusDelegate =
        defaultAudioFocusDelegate(context, options),
    private var timerFactory: Provider<Timer> = defaultTimerFactory()
) {

    @JvmOverloads
    @Deprecated("Access token is unused. Use the constructor that does not require it.")
    constructor(
        context: Context,
        accessToken: String,
        language: String,
        options: VoiceInstructionsPlayerOptions = defaultOptions(),
        audioFocusDelegate: AsyncAudioFocusDelegate =
            defaultAudioFocusDelegate(context, options),
        timerFactory: Provider<Timer> = defaultTimerFactory()
    ) : this(context, language, options, audioFocusDelegate, timerFactory)

    @Deprecated("Access token is unused. Use the constructor that does not require it.")
    constructor(
        context: Context,
        accessToken: String,
        language: String,
        options: VoiceInstructionsPlayerOptions = defaultOptions(),
        audioFocusDelegate: AudioFocusDelegate,
    ) : this(context, language, options, audioFocusDelegate)

    constructor(
        context: Context,
        language: String,
        options: VoiceInstructionsPlayerOptions = defaultOptions(),
        audioFocusDelegate: AudioFocusDelegate,
    ) : this(context, language, options, wrapDelegate(audioFocusDelegate))

    private val attributes: VoiceInstructionsPlayerAttributes =
        VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(options)
    private val playCallbackQueue: Queue<PlayCallback> = ConcurrentLinkedQueue()
    private val filePlayer: VoiceInstructionsFilePlayer =
        VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
            context,
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

            if (currentPlayCallback != null) {
                val currentAnnouncement = currentPlayCallback.announcement
                val currentClientCallback = currentPlayCallback.consumer
                currentClientCallback.accept(currentAnnouncement)
            }
            play()
        }

    /**
     * Change language in runtime.
     *
     * @param language the new [Locale] language (in a format acceptable by [Locale])
     */
    fun updateLanguage(language: String) {
        textPlayer.updateLanguage(language)
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
        logI(VOICE_INSTRUCTION_LOG) {
            "playing $announcement"
        }
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
