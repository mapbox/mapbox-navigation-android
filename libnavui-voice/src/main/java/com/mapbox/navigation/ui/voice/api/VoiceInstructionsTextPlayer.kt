package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.utils.internal.logE
import java.util.Locale

/**
 * Offline implementation of [VoiceInstructionsPlayer].
 * @property context Context
 * @property language [Locale] language (in a format acceptable by [Locale])
 * @property playerAttributes [VoiceInstructionsPlayerAttributes]
 */
internal class VoiceInstructionsTextPlayer(
    private val context: Context,
    private val language: String,
    private val playerAttributes: VoiceInstructionsPlayerAttributes,
) : VoiceInstructionsPlayer {

    @VisibleForTesting
    internal var isLanguageSupported: Boolean = false

    @VisibleForTesting
    internal var textToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            initializeWithLanguage(Locale(language))
        }
    }

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
        callback: VoiceInstructionsPlayerCallback
    ) {
        clientCallback = callback
        check(currentPlay == null) {
            "Only one announcement can be played at a time."
        }
        currentPlay = announcement
        val announcement = announcement.announcement
        if (isLanguageSupported && announcement.isNotBlank()) {
            play(announcement)
        } else {
            logE(
                "$LANGUAGE_NOT_SUPPORTED or announcement from state is blank",
                LOG_CATEGORY
            )
            donePlaying()
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechVolume].
     * Note that this API is not dynamic and only takes effect on the next play announcement.
     * If the volume is set to 0.0f, current play announcement (if any) is stopped though.
     * @param state volume level.
     */
    override fun volume(state: SpeechVolume) {
        volumeLevel = state.level
        if (textToSpeech.isSpeaking && state.level == MUTE_VOLUME_LEVEL) {
            textToSpeech.stop()
        }
    }

    /**
     * Clears any announcements queued.
     */
    override fun clear() {
        textToSpeech.stop()
        currentPlay = null
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        textToSpeech.setOnUtteranceProgressListener(null)
        textToSpeech.shutdown()
        currentPlay = null
        volumeLevel = DEFAULT_VOLUME_LEVEL
    }

    @VisibleForTesting
    internal fun initializeWithLanguage(language: Locale) {
        isLanguageSupported = if (playerAttributes.options.checkIsLanguageAvailable) {
            textToSpeech.isLanguageAvailable(language) == TextToSpeech.LANG_AVAILABLE
        } else {
            true
        }
        if (!isLanguageSupported) {
            logE(LANGUAGE_NOT_SUPPORTED, LOG_CATEGORY)
            return
        }
        textToSpeech.language = language
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                donePlaying()
            }

            override fun onError(utteranceId: String?) {
                // Deprecated, may be called due to https://issuetracker.google.com/issues/138321382
                logE("Unexpected TextToSpeech error", LOG_CATEGORY)
                donePlaying()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                logE("TextToSpeech error: $errorCode", LOG_CATEGORY)
                donePlaying()
            }

            override fun onStart(utteranceId: String?) {
                // Intentionally empty
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                donePlaying()
            }
        })
    }

    private fun donePlaying() {
        currentPlay?.let {
            currentPlay = null
            clientCallback?.onDone(it)
        }
    }

    private fun play(announcement: String) {
        val currentBundle = BundleProvider.retrieveBundle()
        val bundle = currentBundle.apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeLevel)
        }
        playerAttributes.applyOn(textToSpeech, bundle)

        textToSpeech.speak(
            announcement,
            TextToSpeech.QUEUE_FLUSH,
            bundle,
            DEFAULT_UTTERANCE_ID
        )
    }

    private companion object {

        private const val LOG_CATEGORY = "VoiceInstructionsTextPlayer"
        private const val LANGUAGE_NOT_SUPPORTED = "Language is not supported"
        private const val DEFAULT_UTTERANCE_ID = "default_id"
        private const val DEFAULT_VOLUME_LEVEL = 1.0f
        private const val MUTE_VOLUME_LEVEL = 0.0f
    }
}
