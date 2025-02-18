package com.mapbox.navigation.mapgpt.core.textplayer

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mapbox.navigation.mapgpt.core.language.LanguageRepository
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidTextToSpeechVoice(
    val voice: android.speech.tts.Voice,
) : Voice {
    override fun toString(): String {
        return "AndroidTextToSpeechVoice(voice=$voice)"
    }
}

internal class MapGptLocalTTSPlayer(
    context: Context,
    private val languageRepository: LanguageRepository,
    private val playerAttributes: PlayerAttributes,
    private val ttsEngine: String?,
) : LocalTTSPlayer {

    private var textToSpeechInitStatus: Int? = null
    private var volume = 1.0f
    private var language: Language? = null

    private val _availableLanguages = MutableStateFlow(emptySet<Language>())
    override val availableLanguages: StateFlow<Set<Language>> = _availableLanguages.asStateFlow()

    private val _availableVoices = MutableStateFlow(emptySet<Voice>())
    override val availableVoices: StateFlow<Set<Voice>> = _availableVoices.asStateFlow()

    private val textToSpeech =
        TextToSpeech(
            context,
            { status ->
                textToSpeechInitStatus = status
                if (status == TextToSpeech.SUCCESS) {
                    initializeWithLanguage()
                }
            },
            ttsEngine,
        )

    private fun initializeWithLanguage() {
        SharedLog.i(TAG) { "Available tts engines: ${textToSpeech.engines.joinToString()}" }
        val enginePackages = textToSpeech.engines.map { it.name }.toSet()
        if (!enginePackages.contains(ttsEngine)) {
            SharedLog.w(TAG) { "The tts engine was not found: $ttsEngine. Using ${textToSpeech.defaultEngine} instead" }
        } else {
            SharedLog.i(TAG) { "Using tts engine: $ttsEngine" }
        }
        try {
            // there is a chance to get NPE from ITextToSpeechService getting the voices that
            // means we're not able to use TTS
            // https://issuetracker.google.com/issues/37012397?pli=1
            textToSpeech.voices
        } catch (e: NullPointerException) {
            SharedLog.e(TAG) { "Calling textToSpeech.voices causes NullPointerException, " +
                "message: $e" }
            textToSpeechInitStatus = TextToSpeech.ERROR
            return
        }


        val language = languageRepository.language.value
        var locale = language.locale
        val isLanguageSupported = if (playerAttributes.options.checkIsLanguageAvailable) {
            val availableLanguages = textToSpeech.availableLanguages
            SharedLog.i(TAG) { "Available languages: ${availableLanguages.joinToString { it.isO3Language }}" }
            _availableLanguages.value = availableLanguages
                .map { Language(it) }
                .intersect(MapGptVoiceApiClient.availableLanguages)
            SharedLog.i(TAG) {
                "Searching for TTS language: ${locale.isO3Language}"
            }
            val targetTtsLanguage = availableLanguages.firstOrNull {
                it.isO3Language == locale.isO3Language
            }
            SharedLog.i(TAG) {
                "Found TTS language: ${targetTtsLanguage?.isO3Language}"
            }
            targetTtsLanguage?.also { locale = it }
            targetTtsLanguage != null
        } else {
            _availableLanguages.value = setOf(language)
            true
        }
        if (isLanguageSupported) {
            this.language = language
        } else {
            SharedLog.w(TAG) {
                "$locale language is not supported - switching to default " +
                    "$defaultLocale"
            }

            locale = defaultLocale
        }

        updateAvailableVoices(locale)

        SharedLog.d(TAG) { "Set language to $locale" }
        val setLanguageResult = textToSpeech.setLanguage(locale)
        SharedLog.d(TAG) { "setLanguageResult: $setLanguageResult" }
        SharedLog.d(TAG) { "TextToSpeech voice: ${textToSpeech.voice}"}
    }

    private fun updateAvailableVoices(locale: Locale) {
        val availableVoices = textToSpeech.voices.mapNotNull { voice ->
            val voiceLanguage = Language(voice.locale)
            if (voiceLanguage == language) {
                AndroidTextToSpeechVoice(voice)
            } else {
                null
            }
        }.toSet()
        if (availableVoices.isNotEmpty()) {
            _availableVoices.value = availableVoices
        } else {
            val defaultVoice = textToSpeech.voice
            SharedLog.w(TAG) { "No voice for $locale, fallback to $defaultVoice" }
            _availableVoices.value = defaultVoice
                ?.let { setOf(AndroidTextToSpeechVoice(it)) }
                ?: emptySet<AndroidTextToSpeechVoice>()
        }
        SharedLog.d(TAG) { "Available voices: ${_availableVoices.value}" }
    }

    override fun play(
        voice: VoiceAnnouncement.Local,
        callback: PlayerCallback,
    ) {
        val startPosition = voice.progress?.position ?: 0
        val text = voice.text.drop(startPosition)
        SharedLog.d(TAG) { "text: $text" }

        textToSpeech.setOnUtteranceProgressListener(
            UtteranceProgressListenerWrapper(
                text,
                startPosition,
                callback,
            ),
        )
        val bundle = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        if (textToSpeechInitStatus == TextToSpeech.SUCCESS) {
            if (language != languageRepository.language.value) {
                language = languageRepository.language.value
                initializeWithLanguage()
            }
            playerAttributes.applyOn(textToSpeech, bundle)

            textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                bundle,
                voice.utteranceId,
            )
        } else {
            callback.onError(voice.utteranceId, "TTS is not initialized")
        }
    }

    override fun stop() {
        textToSpeech.stop()
    }

    override fun volume(level: Float) {
        volume = level
    }

    inner class UtteranceProgressListenerWrapper(
        private val text: String?,
        private val startPosition: Int,
        private val clientCallback: PlayerCallback,
    ) : UtteranceProgressListener() {

        private var currentPosition: Int = 0
        override fun onStart(utteranceId: String) {
            SharedLog.d(TAG) { "onStart $utteranceId" }
            clientCallback.onStartPlaying(text = text, utteranceId = utteranceId)
        }

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            SharedLog.d(TAG) { "onRangeStart $utteranceId, start: $start, end $end, frame $frame" }
            currentPosition = startPosition + start
        }

        override fun onDone(utteranceId: String) {
            SharedLog.d(TAG) { "onDone $utteranceId" }
            clientCallback.onComplete(text = text, utteranceId = utteranceId)
            currentPosition = 0
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
            // Deprecated, may be called due to https://issuetracker.google.com/issues/138321382
            SharedLog.d(TAG) { "onError $utteranceId" }
            clientCallback.onError(utteranceId, null)
            currentPosition = 0
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            SharedLog.d(TAG) { "onError $utteranceId, errorCode: $errorCode" }
            clientCallback.onError(utteranceId, errorCode.toString())
            currentPosition = 0
        }

        override fun onStop(utteranceId: String, interrupted: Boolean) {
            SharedLog.d(TAG) { "onStop $utteranceId, interrupted: $interrupted" }
            clientCallback.onStop(
                utteranceId,
                VoiceProgress.Index(currentPosition),
            )
            currentPosition = 0
        }
    }

    private companion object {

        private const val TAG = "LocalTTSPlayer"
        private val defaultLocale = Locale.US
    }
}
