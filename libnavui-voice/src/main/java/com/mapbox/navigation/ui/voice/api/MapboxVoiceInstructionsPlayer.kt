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
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Queue
import java.util.Timer
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.schedule

/**
 * Hybrid implementation of [MapboxVoiceInstructionsPlayer] combining [VoiceInstructionsTextPlayer] and
 * [VoiceInstructionsFilePlayer] speech players.
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

    private val scope = InternalJobControlFactory.createMainScopeJobControl().scope

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
    private var initialized = false
    private val textPlayer: VoiceInstructionsTextPlayer =
        VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
            context,
            language,
            attributes,
        ) {
            initialized = true
            play()
        }

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
     * If 500 ms pass between the moment the instruction was removed from the queue to be played
     * and the moment the playing actually starts, it will be cancelled.
     * As an alternative, you may manually invoke the [cancel] method as soon as playing is
     * no longer required.
     *
     * @param announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param consumer represents that the speech player is done playing
     */
    fun play(
        announcement: SpeechAnnouncement,
        consumer: MapboxNavigationConsumer<SpeechAnnouncement>
    ) {
        play(announcement, consumer, 500)
    }

    /**
     * Given [SpeechAnnouncement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param consumer represents that the speech player is done playing
     * @param expirationMillis if the specified timeout passes between the moment the instruction
     *  was removed from the queue to be played and the moment the playing actually starts,
     *  it will be cancelled. As an alternative, you may manually invoke the [cancel] method
     *  as soon as playing is no longer required. If null is provided, it will never be cancelled.
     */
    fun play(
        announcement: SpeechAnnouncement,
        consumer: MapboxNavigationConsumer<SpeechAnnouncement>,
        expirationMillis: Long?
    ) {
        playCallbackQueue.add(PlayCallback(announcement, consumer, expirationMillis))
        if (playCallbackQueue.size == 1) {
            play()
        }
    }

    /**
     * Cancels playing the given announcement. Use in cases when playing the instruction
     * after some time or after a certain event becomes useless.
     * As an alternative, you may pass `expirationMillis` parameter to [play] method.
     * It will cancel the playing automatically after the specified timeout.
     *
     * @param announcement the instruction to be cancelled
     */
    fun cancel(announcement: SpeechAnnouncement) {
        filePlayer.cancel(announcement)
        textPlayer.cancel(announcement)
        playCallbackQueue.removeIf {
            (it.announcement == announcement).also { result ->
                if (result) {
                    it.consumer.accept(it.announcement)
                }
            }
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
        if (!initialized) {
            currentPlayCallback.expirationMillis?.let {
                scope.launch {
                    delay(it)
                    // cancel is harmless if instruction is already played
                    cancel(currentPlayCallback.announcement)
                }
            }
            return
        }

        val currentPlay = currentPlayCallback.announcement
        val owner = when (currentPlay.file) {
            null -> AudioFocusOwner.TextToSpeech
            else -> AudioFocusOwner.MediaPlayer
        }

        requestFocus(owner) { isGranted ->
            if (isGranted) {
                val player = when (owner) {
                    AudioFocusOwner.MediaPlayer -> filePlayer
                    AudioFocusOwner.TextToSpeech -> textPlayer
                }
                player.play(currentPlay, doneCallback)
            } else {
                doneCallback.onDone(currentPlay)
            }
        }
    }

    private fun finalize() {
        playCallbackQueue.clear()
        scope.cancel()
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
