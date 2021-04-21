package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.utils.internal.LoggerProvider
import java.util.Locale

/**
 * Offline implementation of [VoiceInstructionsPlayer].
 * @property context Context
 * @property language [Locale] language (ISO 639)
 */
internal class VoiceInstructionsTextPlayer(
    private val context: Context,
    private val language: String
) : VoiceInstructionsPlayer {

    private var isLanguageSupported: Boolean = false
    private val textToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            initializeWithLanguage(Locale(language))
        }
    }
    private var volumeLevel: Float = DEFAULT_VOLUME_LEVEL
    private var clientCallback: VoiceInstructionsPlayerCallback? = null
    private var currentPlay: SpeechAnnouncement? = null

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
            LoggerProvider.logger.e(
                Tag(TAG),
                Message("$LANGUAGE_NOT_SUPPORTED or announcement from state is blank")
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
        volumeLevel = DEFAULT_VOLUME_LEVEL
    }

    private fun initializeWithLanguage(language: Locale) {
        isLanguageSupported =
            textToSpeech.isLanguageAvailable(language) == TextToSpeech.LANG_AVAILABLE
        if (!isLanguageSupported) {
            LoggerProvider.logger.e(Tag(TAG), Message(LANGUAGE_NOT_SUPPORTED))
            return
        }
        textToSpeech.language = language
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                donePlaying()
            }

            override fun onError(utteranceId: String?) {
                // Intentionally empty as deprecated
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                LoggerProvider.logger.e(Tag(TAG), Message("TextToSpeech error: $errorCode"))
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
        val bundle = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeLevel)
        }
        textToSpeech.speak(
            announcement,
            TextToSpeech.QUEUE_FLUSH,
            bundle,
            DEFAULT_UTTERANCE_ID
        )
    }

    private companion object {
        private const val TAG = "MbxVoiceInstructionsTextPlayer"
        private const val LANGUAGE_NOT_SUPPORTED = "Language is not supported"
        private const val DEFAULT_UTTERANCE_ID = "default_id"
        private const val DEFAULT_VOLUME_LEVEL = 1.0f
        private const val MUTE_VOLUME_LEVEL = 0.0f
    }
}
