package com.mapbox.navigation.mapgpt.core.userinput

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.Locale

class SpeechRecognizerOwner : UserInputOwnerMiddleware,
    CoroutineMiddleware<UserInputMiddlewareContext>() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var listenIntent: Intent

    private val _state = MutableStateFlow<UserInputState>(UserInputState.Idle)

    override val provider = UserInputProvider.SpeechRecognizer

    override val state = _state.asStateFlow()

    private val _availableLanguages = MutableStateFlow<Set<Language>>(emptySet())
    override val availableLanguages: StateFlow<Set<Language>> = _availableLanguages.asStateFlow()

    override fun onAttached(middlewareContext: UserInputMiddlewareContext) {
        super.onAttached(middlewareContext)
        SharedLog.i(TAG) { "onAttached" }
        middlewareContext.isReachable.onEach { isReachable ->
            _availableLanguages.value = if (isReachable) {
                remoteLanguages
            } else {
                setOf(Language(Locale.getDefault()), middlewareContext.language.value)
            }
        }.launchIn(mainScope)
        val applicationContext = middlewareContext.platformContext.applicationContext
        if (SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            SharedLog.i(TAG) { "Speech recognition is available" }
        } else {
            SharedLog.e(TAG) { "Speech recognition is not available" }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        speechRecognizer?.setRecognitionListener(recognitionListener)
        combine(
            middlewareContext.isReachable,
            middlewareContext.language,
        ) { isReachableValue, languageValue ->
            listenIntent = updateListenIntent(
                context = applicationContext,
                isConnected = isReachableValue,
                languageTag = languageValue.languageTag,
            )
        }.launchIn(mainScope)
    }

    override fun onDetached(middlewareContext: UserInputMiddlewareContext) {
        super.onDetached(middlewareContext)
        SharedLog.i(TAG) { "onDetached" }
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = UserInputState.Idle
    }

    private val recognitionListener: RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle) {
            SharedLog.i(TAG) { "onReadyForSpeech ${params.keySet().joinToString()}" }
            _state.value = UserInputState.Listening(text = null)
        }

        override fun onBeginningOfSpeech() {
            SharedLog.i(TAG) { "onBeginningOfSpeech" }
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Continuous updates of the sound level in the audio stream.
        }

        override fun onBufferReceived(buffer: ByteArray) {
            SharedLog.i(TAG) { "onBufferReceived" }
        }

        override fun onEndOfSpeech() {
            SharedLog.i(TAG) { "onEndOfSpeech" }
        }

        override fun onError(error: Int) {
            val errorString = errorToString(error)
            // This is emitting error state even if we have a result
            val validError = error != SpeechRecognizer.ERROR_CLIENT
            SharedLog.i(TAG) { "onError $errorString validError $validError" }
            if (!validError) return
            _state.update { state ->
                when {
                    state is UserInputState.Result -> state
                    error == SpeechRecognizer.ERROR_NO_MATCH -> UserInputState.Idle
                    else -> UserInputState.Error(errorString)
                }
            }
        }

        override fun onResults(results: Bundle) {
            // Called when recognition results are available
            val resultsText = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?: emptyList()
            val text = resultsText.firstOrNull() ?: ""
            speechRecognizer?.stopListening()
            val state = if (text.isNotBlank()) {
                UserInputState.Result(text)
            } else {
                UserInputState.Idle
            }
            SharedLog.i(TAG) { "onResults $resultsText" }
            _state.value = state
        }

        override fun onPartialResults(partialResults: Bundle) {
            val results = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            SharedLog.i(TAG) { "onPartialResults results: ${results?.joinToString()}" }
            _state.value = UserInputState.Listening(text = results?.firstOrNull())
        }

        override fun onEvent(eventType: Int, params: Bundle) {
            SharedLog.i(TAG) { "onEvent $eventType $params" }
        }

        private fun errorToString(error: Int): String {
            return when (error) {
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "error_network_timeout"
                SpeechRecognizer.ERROR_NETWORK -> "error_network"
                SpeechRecognizer.ERROR_AUDIO -> "error_audio"
                SpeechRecognizer.ERROR_SERVER -> "error_server"
                SpeechRecognizer.ERROR_CLIENT -> "error_client"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "error_speech_timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "error_no_match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "error_recognizer_busy"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "error_insufficient_permissions"
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "error_too_many_requests"
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "error_server_disconnected"
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "error_language_not_supported"
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "error_language_unavailable"
                SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "error_cannot_check_support"
                else -> "error_unknown"
            }
        }
    }

    override fun startListening() {
        SharedLog.i(TAG) { "startListening" }
        _state.value = UserInputState.Listening(text = null)
        speechRecognizer().startListening(listenIntent)
    }

    override fun stopListening() {
        SharedLog.i(TAG) { "stopListening" }
        speechRecognizer?.stopListening()
        _state.value = UserInputState.Idle
    }

    /**
     * Provides access to the [SpeechRecognizer] to perform manual operations. This will throw an
     * exception if it is accessed when it is not available.
     */
    @Throws(IllegalStateException::class)
    fun speechRecognizer(): SpeechRecognizer {
        val speechRecognizer = this.speechRecognizer
        checkNotNull(speechRecognizer) {
            "Make sure the SpeechRecognizerOwner is attached before use."
        }
        return speechRecognizer
    }

    private fun updateListenIntent(
        context: Context,
        isConnected: Boolean,
        languageTag: String?,
    ): Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        // Use the device language if there is no network connection. Can use any language when
        // the network is connected.
        if (languageTag != null && isConnected) {
            // for some reason, using the 'BCP 47' standard described in the Intent docs doesn't work
            // however, using the 'ISO 639' does work
            SharedLog.i(TAG) { "Set language to $languageTag; tag: $languageTag" }
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                languageTag,
            )
        }
        return intent
    }

    private companion object {
        private const val TAG = "SpeechRecognizerOwner"

        private val remoteLanguages: Set<Language> = Locale.getAvailableLocales()
            .map(::Language)
            .toSet()
    }
}
