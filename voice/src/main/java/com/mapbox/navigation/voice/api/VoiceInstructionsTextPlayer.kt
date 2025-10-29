package com.mapbox.navigation.voice.api

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.VisibleForTesting
import androidx.core.os.trace
import com.mapbox.navigation.utils.internal.InternalJobControlFactory.createDefaultScopeJobControl
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechVolume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Offline implementation of [VoiceInstructionsPlayer].
 * @param context Context
 * @param language [Locale] language (in a format acceptable by [Locale])
 * @param playerAttributes [VoiceInstructionsPlayerAttributes]
 */
internal class VoiceInstructionsTextPlayer(
    private val context: Context,
    private var language: String,
    private val playerAttributes: VoiceInstructionsPlayerAttributes,
) : VoiceInstructionsPlayer {

    @VisibleForTesting
    internal var isLanguageSupported: Boolean = false

    private val jobControl = createDefaultScopeJobControl()

    private val textToSpeech = CompletableDeferred<TextToSpeech>()
    private val textToSpeechStatus = CompletableDeferred<Int>()

    @VisibleForTesting
    internal var volumeLevel: Float = DEFAULT_VOLUME_LEVEL
    private var clientCallback: VoiceInstructionsPlayerCallback? = null

    @VisibleForTesting
    internal var currentPlay: SpeechAnnouncement? = null

    init {
        initTextToSpeech()
        initListenersOnceReady()
    }

    fun updateLanguage(language: String) {
        this.language = language
        jobControl.scope.launch {
            val tts = awaitTextToSpeech() ?: return@launch
            initializeWithLanguage(Locale(language), tts)
        }
    }

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
        val text = announcement.announcement
        if (isLanguageSupported && text.isNotBlank()) {
            play(text)
        } else {
            logE { "$LANGUAGE_NOT_SUPPORTED or announcement from state is blank" }
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
        jobControl.scope.launch {
            val tts = awaitTextToSpeech() ?: return@launch
            if (tts.isSpeaking && state.level == MUTE_VOLUME_LEVEL) {
                tts.stop()
            }
        }
    }

    /**
     * Clears any announcements queued.
     */
    override fun clear() {
        jobControl.scope.launch {
            val tts = getTextToSpeechOrNull() ?: return@launch
            if (tts.isSpeaking) {
                tts.stop()
            }
        }
        currentPlay = null
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        jobControl.job.cancelChildren()
        getTextToSpeechOrNull()?.let {
            it.setOnUtteranceProgressListener(null)
            it.shutdown()
        }
        textToSpeech.cancel()
        textToSpeechStatus.cancel()
        currentPlay = null
        volumeLevel = DEFAULT_VOLUME_LEVEL
    }

    @VisibleForTesting
    internal suspend fun awaitTextToSpeech(): TextToSpeech? {
        return try {
            val tts = textToSpeech.await()
            val status = textToSpeechStatus.await()
            tts.takeIf { status == TextToSpeech.SUCCESS }
        } catch (e: Exception) {
            null
        }
    }

    private fun getTextToSpeechOrNull(): TextToSpeech? {
        return if (textToSpeech.isCompleted) {
            textToSpeech.getCompleted()
        } else {
            null
        }
    }

    private fun initializeWithLanguage(
        language: Locale,
        tts: TextToSpeech,
    ) {
        trace(TRACE_INIT_LANG) {
            isLanguageSupported = if (playerAttributes.options.checkIsLanguageAvailable) {
                tts.isLanguageAvailable(language) == TextToSpeech.LANG_AVAILABLE
            } else {
                true
            }
            if (!isLanguageSupported) {
                logE { LANGUAGE_NOT_SUPPORTED }
                return@trace
            }
            tts.language = language
        }
    }

    private fun setUpUtteranceProgressListener(
        tts: TextToSpeech,
    ) {
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onDone(utteranceId: String?) {
                    donePlaying()
                }

                override fun onError(utteranceId: String?) {
                    // Deprecated, may be called due to https://issuetracker.google.com/issues/138321382
                    logE { "Unexpected TextToSpeech error" }
                    donePlaying()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    logE { "TextToSpeech error: $errorCode" }
                    donePlaying()
                }

                override fun onStart(utteranceId: String?) {
                    // Intentionally empty
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    donePlaying()
                }
            },
        )
    }

    private fun donePlaying() {
        currentPlay?.let {
            currentPlay = null
            clientCallback?.onDone(it)
        }
    }

    private fun play(announcement: String) {
        logD { "play: $announcement" }
        jobControl.scope.launch {
            trace(TRACE_PLAY) {
                val currentBundle = BundleProvider.retrieveBundle()
                val bundle = currentBundle.apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeLevel)
                }
                val tts = awaitTextToSpeech() ?: return@launch
                playerAttributes.applyOn(tts, bundle)

                tts.speak(
                    announcement,
                    TextToSpeech.QUEUE_FLUSH,
                    bundle,
                    DEFAULT_UTTERANCE_ID,
                )
            }
        }
    }

    private fun initTextToSpeech() {
        trace(TRACE_GET_TTS) {
            textToSpeech.complete(
                TextToSpeechProvider.getTextToSpeech(
                    context.applicationContext,
                ) { status ->
                    textToSpeechStatus.complete(status)
                },
            )
        }
    }

    private fun initListenersOnceReady() {
        jobControl.scope.launch {
            val tts = awaitTextToSpeech() ?: return@launch
            initializeWithLanguage(Locale(language), tts)
            setUpUtteranceProgressListener(tts)
        }
    }

    private companion object {

        private const val LOG_CATEGORY = "VoiceInstructionsTextPlayer"
        private const val LANGUAGE_NOT_SUPPORTED = "Language is not supported"
        private const val DEFAULT_UTTERANCE_ID = "default_id"
        private const val DEFAULT_VOLUME_LEVEL = 1.0f
        private const val MUTE_VOLUME_LEVEL = 0.0f

        private const val TRACE_GET_TTS = "VoiceInstructionsTextPlayer.getTextToSpeech"
        private const val TRACE_INIT_LANG = "VoiceInstructionsTextPlayer.initializeWithLanguage"
        private const val TRACE_PLAY = "VoiceInstructionsTextPlayer.play"

        private inline fun logD(msg: () -> String) = logD(LOG_CATEGORY, msg)
        private inline fun logE(msg: () -> String) = logE(LOG_CATEGORY, msg)
    }
}
