package com.mapbox.navigation.mapgpt.core.api

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.microphone.PlatformMicrophone
import com.mapbox.navigation.mapgpt.core.textplayer.Sound
import com.mapbox.navigation.mapgpt.core.textplayer.SoundPlayer
import com.mapbox.navigation.mapgpt.core.userinput.UserInputMiddlewareContext
import com.mapbox.navigation.mapgpt.core.userinput.UserInputOwnerMiddleware
import com.mapbox.navigation.mapgpt.core.userinput.UserInputProvider
import com.mapbox.navigation.mapgpt.core.userinput.UserInputState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class MapboxUserInputMiddleware(
    override val provider: UserInputProvider = UserInputProvider.MapboxASR,
) : UserInputOwnerMiddleware, CoroutineMiddleware<UserInputMiddlewareContext>() {

    private val _state = MutableStateFlow<UserInputState>(UserInputState.Idle)
    override val state: StateFlow<UserInputState> = _state.asStateFlow()

    private val _availableLanguages = MutableStateFlow(emptySet<Language>())
    override val availableLanguages: StateFlow<Set<Language>> = _availableLanguages.asStateFlow()

    override fun startListening() {
        if (!isAttached) {
            val errorReason = "Cannot startListening when middleware is detached."
            SharedLog.e(TAG) { errorReason }
            _state.value = UserInputState.Error(errorReason)
            return
        }
        SharedLog.i(TAG) { "startListening" }
        when (state.value) {
            is UserInputState.Listening -> SharedLog.i(TAG) { "already listening" }
            is UserInputState.Result,
            is UserInputState.Idle,
            is UserInputState.Error,
            -> {
                _state.value = UserInputState.Listening(text = null)
            }
        }
    }

    override fun stopListening() {
        SharedLog.i(TAG) { "stopListening" }
        _state.value = UserInputState.Idle
        middlewareContext?.mapGptService?.sendFinalAsrData()
    }

    override fun onAttached(middlewareContext: UserInputMiddlewareContext) {
        super.onAttached(middlewareContext)
        SharedLog.i(TAG) { "onAttached" }
        _state.value = UserInputState.Idle
        _availableLanguages.value = remoteLanguages
        launchStateLogging()
        launchSoundsForStateChanges(middlewareContext.soundPlayer)
        launchMicrophoneErrorCheck(middlewareContext.microphone)
        launchStopSpeakingCheck()
        launchRecorder(middlewareContext)
    }

    override fun onDetached(middlewareContext: UserInputMiddlewareContext) {
        super.onDetached(middlewareContext)
        SharedLog.i(TAG) { "onDetached" }
        middlewareContext.microphone.stop()
        _state.value = UserInputState.Idle
    }

    fun onTranscriptReceived(transcript: String, isFinal: Boolean) {
        _state.value = when {
            !isFinal -> { UserInputState.Listening(text = transcript) }
            transcript.isNotBlank() -> { UserInputState.Result(transcript,true) }
            else -> UserInputState.Idle
        }
    }

    private fun launchStateLogging() {
        state.onEach { state ->
            SharedLog.d(TAG) { "state: $state" }
        }.launchIn(mainScope)
    }

    private fun launchSoundsForStateChanges(soundPlayer: SoundPlayer) = ioScope.launch {
        state.reduce { previous, current ->
            when (current) {
                is UserInputState.Result -> soundPlayer.playSound(soundCaptured)
                is UserInputState.Listening -> if (previous is UserInputState.Idle) {
                    soundPlayer.playSound(soundOn)
                }
                is UserInputState.Idle,
                is UserInputState.Error,
                -> if (previous is UserInputState.Listening) {
                    soundPlayer.playSound(soundOff)
                }
            }
            soundPlayer.release()
            current
        }
    }

    private fun launchMicrophoneErrorCheck(microphone: PlatformMicrophone) {
        microphone.state.filterIsInstance<PlatformMicrophone.State.Error>().onEach { error ->
            val stateError = UserInputState.Error("Microphone error: ${error.reason}")
            SharedLog.w(TAG) { stateError.reason }
            _state.value = stateError
        }.launchIn(ioScope)
    }

    /**
     * While in listening state, check if the user has stopped speaking.
     */
    @OptIn(ExperimentalTime::class)
    private fun launchStopSpeakingCheck() = mainScope.launch(Dispatchers.Default) {
        state.collectLatest { userInputState ->
            while (userInputState is UserInputState.Listening) {
                val elapsed = userInputState.timeMark.elapsedNow()
                if (elapsed >= STOPPED_SPEAKING_THRESHOLD) {
                    val transcription = (state.value as? UserInputState.Listening)?.text ?: ""
                    SharedLog.i(TAG) { "User has stopped speaking" }
                    if (transcription.isEmpty()) {
                        _state.value = UserInputState.Idle
                    }
                }
                delay(CHECK_SPEAKING_INTERVAL)
            }
        }
    }

    private fun launchRecorder(middlewareContext: UserInputMiddlewareContext) {
        // It makes sense to use an if/else in the collectLatest block to stop the
        // stream when the state changes to something other than listening. But it is not
        // guaranteed to work because the microphone may swallow and hold onto the cancellation.
        // Here we use a separate coroutines ensure microphone.stop() is called even if the
        // microphone holds onto the coroutine.
        ioScope.launch {
            state.map { it is UserInputState.Listening }
                .distinctUntilChanged()
                .collectLatest { isListening ->
                    if (isListening) {
                        SharedLog.d(TAG) { "Start streamAudioFromMicrophone" }
                        streamAudioFromMicrophone(middlewareContext)
                        SharedLog.d(TAG) { "Ended streamAudioFromMicrophone" }
                    }
                }
        }
        state.onEach { state ->
            if (state !is UserInputState.Listening) {
                middlewareContext.microphone.stop()
            }
        }.launchIn(mainScope)
    }

    private suspend fun streamAudioFromMicrophone(
        middlewareContext: UserInputMiddlewareContext,
    ) {
        if (!middlewareContext.microphone.hasPermission()) {
            _state.value = UserInputState.Error("Microphone permission is not granted.")
            return
        }
        val language = middlewareContext.language.value
        SharedLog.i(TAG) { "streamAudioFromMicrophone language: ${language.languageTag}" }

        middlewareContext.mapGptContextProvider.getContext()?.let {
            middlewareContext.mapGptService.startAsrRequest(
                contextDTO = it,
                capabilities = middlewareContext.capabilitiesRepository.capabilityIds(),
            )
        } ?: return

        middlewareContext.microphone.stream { chunk ->
            if (state.value !is UserInputState.Listening) {
                middlewareContext.microphone.stop()
                SharedLog.d(TAG) { "Microphone has now stopped listening with state value: ${state.value}" }
                return@stream
            }
            SharedLog.d(TAG) { "Streaming chunk: ${chunk.bytesRead}" }
            middlewareContext.mapGptService.sendAsrData(chunk.byteArray)
        }
    }

    private companion object {
        private const val TAG = "MapboxUserInputMiddleware"

        private val soundOn = Sound.CustomSound("ding-stt-on.mp3")
        private val soundOff = Sound.CustomSound("ding-stt-off.mp3")
        private val soundCaptured = Sound.CustomSound("ding-stt-captured.mp3")

        private val STOPPED_SPEAKING_THRESHOLD = 3.toDuration(DurationUnit.SECONDS)
        private val CHECK_SPEAKING_INTERVAL = 1.toDuration(DurationUnit.SECONDS)

        private val SUPPORTED_LANGUAGES = setOf(
            "af-ZA",
            "am-ET",
            "ar-AE",
            "ar-BH",
            "ar-DZ",
            "ar-EG",
            "ar-IL",
            "ar-IQ",
            "ar-JO",
            "ar-KW",
            "ar-LB",
            "ar-MA",
            "ar-MR",
            "ar-OM",
            "ar-PS",
            "ar-QA",
            "ar-SA",
            "ar-TN",
            "ar-YE",
            "az-AZ",
            "bg-BG",
            "bn-BD",
            "bn-IN",
            "bs-BA",
            "ca-ES",
            "cs-CZ",
            "da-DK",
            "de-AT",
            "de-CH",
            "de-DE",
            "el-GR",
            "en-AU",
            "en-CA",
            "en-GB",
            "en-GH",
            "en-HK",
            "en-IE",
            "en-IN",
            "en-KE",
            "en-NG",
            "en-NZ",
            "en-PH",
            "en-PK",
            "en-SG",
            "en-TZ",
            "en-US",
            "en-ZA",
            "es-AR",
            "es-BO",
            "es-CL",
            "es-CO",
            "es-CR",
            "es-DO",
            "es-EC",
            "es-ES",
            "es-GT",
            "es-HN",
            "es-MX",
            "es-NI",
            "es-PA",
            "es-PE",
            "es-PR",
            "es-PY",
            "es-SV",
            "es-US",
            "es-UY",
            "es-VE",
            "et-EE",
            "eu-ES",
            "fa-IR",
            "fi-FI",
            "fil-PH",
            "fr-BE",
            "fr-CA",
            "fr-CH",
            "fr-FR",
            "gl-ES",
            "gu-IN",
            "hi-IN",
            "hr-HR",
            "hu-HU",
            "hy-AM",
            "id-ID",
            "is-IS",
            "it-CH",
            "it-IT",
            "iw-IL",
            "ja-JP",
            "jv-ID",
            "ka-GE",
            "kk-KZ",
            "km-KH",
            "kn-IN",
            "ko-KR",
            "lo-LA",
            "lt-LT",
            "lv-LV",
            "mk-MK",
            "ml-IN",
            "mn-MN",
            "mr-IN",
            "ms-MY",
            "my-MM",
            "ne-NP",
            "nl-BE",
            "nl-NL",
            "no-NO",
            "pa-Guru-IN",
            "pl-PL",
            "pt-BR",
            "pt-PT",
            "ro-RO",
            "ru-RU",
            "rw-RW",
            "si-LK",
            "sk-SK",
            "sl-SI",
            "sq-AL",
            "sr-RS",
            "ss-Latn-ZA",
            "st-ZA",
            "su-ID",
            "sv-SE",
            "sw-KE",
            "sw-TZ",
            "ta-IN",
            "ta-LK",
            "ta-MY",
            "ta-SG",
            "te-IN",
            "th-TH",
            "tn-Latn-ZA",
            "tr-TR",
            "ts-ZA",
            "uk-UA",
            "ur-IN",
            "ur-PK",
            "uz-UZ",
            "ve-ZA",
            "vi-VN",
            "xh-ZA",
            "yue-Hant-HK",
            "zh",
            "cmn-Hans-CN",
            "zh-TW",
            "cmn-Hant-TW",
            "zu-ZA",
        )
        val remoteLanguages = SUPPORTED_LANGUAGES.map { Language(it) }.toSet()
    }
}
