package com.mapbox.navigation.voicefeedback.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.voicefeedback.ASRState
import com.mapbox.navigation.voicefeedback.Microphone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxAutomaticSpeechRecognitionEngine(
    private val mapboxASRService: MapboxASRService,
    private val microphone: Microphone,
    private val scope: CoroutineScope = MainScope(),
    private val stoppedSpeakingThreshold: Duration = STOPPED_SPEAKING_THRESHOLD,
    private val checkSpeakingInterval: Duration = CHECK_SPEAKING_INTERVAL,
    private val resultTimeout: Duration = RESULT_TIMEOUT,
) : AutomaticSpeechRecognitionEngine {

    private val listeningState = MutableStateFlow(value = false)
    override val state = MutableStateFlow<ASRState?>(null)

    private var lastKnownStopSpeakingState: ASRState? = null

    init {
        scope.launch(Dispatchers.Main) {
            mapboxASRService.sessionState
                .map {
                    when (it) {
                        is AsrSessionState.Connected -> ASRState.Idle
                        is AsrSessionState.Connecting, AsrSessionState.Disconnected -> null
                    }
                }.collect {
                    state.value = it
                }
        }
        scope.launch(Dispatchers.Main) {
            combine(
                listeningState,
                mapboxASRService.asrData,
            ) { isListening, asrData ->
                logD(TAG) { "listeningState: $isListening asrData: $asrData" }
                when {
                    asrData is AsrData.Result ->
                        ASRState.Result(asrData.description, asrData.type)

                    isListening && asrData is AsrData.Transcript -> {
                        if (asrData.isFinal) {
                            stopListening()
                            ASRState.SpeechFinishedWaitingForResult
                        } else {
                            ASRState.Listening(asrData.text)
                        }
                    }

                    else -> null
                }
            }.filterNotNull().collect { asrState ->
                logD(TAG) { "newAsrState: $asrState" }
                state.value = asrState
            }
        }
        scope.launch(Dispatchers.Main) {
            microphone.state
                .filterIsInstance<Microphone.State.Error>()
                .collect { microphoneErrorState ->
                    logD(TAG) { "Microphone error handled: $microphoneErrorState" }
                    state.value = ASRState.Error(RuntimeException(microphoneErrorState.reason))
                }
        }

        scope.launch(Dispatchers.IO) {
            listeningState.collectLatest { isListening ->
                logD(TAG) { "stream start: $isListening" }
                if (isListening) {
                    microphone.stream { streaming ->
                        val streamingIsListening = listeningState.value
                        if (!streamingIsListening) {
                            logD(TAG) { "Microphone has now stopped listening" }
                            microphone.stop()
                            return@stream
                        }
                        sendAsrData(streaming)
                    }
                } else {
                    microphone.stop()
                }
            }
        }
        launchAsrTimeoutMonitor()
    }

    private fun launchAsrTimeoutMonitor() = scope.launch(Dispatchers.Main) {
        state.collectLatest { asrState ->
            logI(TAG) { "launchAsrTimeoutMonitor: $asrState" }
            when (asrState) {
                is ASRState.Listening -> {
                    while (true) {
                        val elapsed = try {
                            asrState.elapsedTime()
                        } catch (iae: IllegalArgumentException) {
                            stoppedSpeakingThreshold
                        }
                        if (elapsed >= stoppedSpeakingThreshold) {
                            val stateValue = state.value
                            val transcription = (stateValue as? ASRState.Listening)?.text.orEmpty()
                            logI(TAG) { "User has stopped speaking: $transcription" }
                            logI(TAG) {
                                "User has stopped speaking. " +
                                    "State: $stateValue LastKnownState: $lastKnownStopSpeakingState"
                            }
                            listeningState.value = false
                            if (transcription.isBlank() ||
                                lastKnownStopSpeakingState == stateValue
                            ) {
                                logI(TAG) { "[MapboxASREngine]. InterruptedByTimeout" }
                                state.value = ASRState.InterruptedByTimeout
                                lastKnownStopSpeakingState = null
                            } else {
                                lastKnownStopSpeakingState = stateValue
                            }
                        }
                        delay(checkSpeakingInterval)
                    }
                }
                is ASRState.SpeechFinishedWaitingForResult -> {
                    delay(resultTimeout)
                    logI(TAG) {
                        "No server response received within $resultTimeout. " +
                            "Possible causes: speech classified as non-feedback by backend, " +
                            "or loss of connectivity."
                    }
                    state.value = ASRState.InterruptedByTimeout
                }
                else -> { /* no-op */ }
            }
        }
    }

    override fun startListening() {
        logD(TAG) { "startListening" }
        mapboxASRService.startAsrRequest()
        state.value = ASRState.Listening("")
        listeningState.value = true
    }

    override fun stopListening() {
        logD(TAG) { "stopListening" }
        listeningState.value = false
        state.value = ASRState.Idle
        mapboxASRService.sendFinalAsrData(false)
    }

    override fun interruptListening() {
        logD(TAG) { "interruptListening" }
        listeningState.value = false
        state.value = ASRState.Interrupted
        mapboxASRService.sendFinalAsrData(true)
    }

    override fun connect(token: String) {
        mapboxASRService.connect(token)
    }

    override fun disconnect() {
        scope.launch {
            mapboxASRService.disconnect()
        }
    }

    private fun sendAsrData(streaming: Microphone.State.Streaming) {
        mapboxASRService.sendAsrData(streaming.byteArray)
    }

    companion object {

        private const val TAG = "MapboxAutomaticSpeechRecognitionEngine"

        private val STOPPED_SPEAKING_THRESHOLD = 6.toDuration(DurationUnit.SECONDS)
        private val CHECK_SPEAKING_INTERVAL = 1.toDuration(DurationUnit.SECONDS)
        private val RESULT_TIMEOUT = 5.toDuration(DurationUnit.SECONDS)
    }
}
