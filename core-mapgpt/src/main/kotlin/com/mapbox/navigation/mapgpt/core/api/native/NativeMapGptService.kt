package com.mapbox.navigation.mapgpt.core.api.native

import com.mapbox.mapgpt.experimental.MapgptConfiguration
import com.mapbox.navigation.mapgpt.api.native.MapGptSessionType
import com.mapbox.navigation.mapgpt.core.MapGptEndpoint
import com.mapbox.navigation.mapgpt.core.api.ConversationState
import com.mapbox.navigation.mapgpt.core.api.ConversationStateProcessor
import com.mapbox.navigation.mapgpt.core.api.ConversationStateProcessorImpl
import com.mapbox.navigation.mapgpt.core.api.InteractionHistoryElement
import com.mapbox.navigation.mapgpt.core.api.MapGptContextDTO
import com.mapbox.navigation.mapgpt.core.api.MapGptService
import com.mapbox.navigation.mapgpt.core.api.MapGptStreamingRequest
import com.mapbox.navigation.mapgpt.core.api.Result
import com.mapbox.navigation.mapgpt.core.api.SessionFrame
import com.mapbox.navigation.mapgpt.core.api.SessionState
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.internaltools.ReplayRecorder
import com.mapbox.navigation.mapgpt.core.language.LanguageRepository
import com.mapbox.navigation.mapgpt.core.userinput.UserInputMiddlewareManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Native [MapGptService] implementation.
 *
 * @param coroutineScope native map gpt coroutine scope that might be overridden on demand
 */
class NativeMapGptService(
    processingTimeout: Duration = DEFAULT_PROCESSING_TIMEOUT,
    userInputMiddlewareManager: UserInputMiddlewareManager,
    private val profileId: String,
    private val languageRepository: LanguageRepository,
    private val coroutineScope: CoroutineScope = MainScope(),
) : MapGptService, NativePlatformMapGptObserver {

    private val jsonParser = Json
    private var connectionJob: Job? = null
    private val mapGptSessionConnectionMutex = Mutex()
    private val platformServiceWrapper = NativePlatformMapGptServiceWrapper(
        middlewareManager = userInputMiddlewareManager,
        observer = this,
        cancelTimeout = DEFAULT_CANCEL_TIMEOUT,
    )
    private val conversationStateProcessor: ConversationStateProcessor =
        ConversationStateProcessorImpl(
            coroutineScope = coroutineScope,
            timeout = processingTimeout,
        )

    override val sessionState = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val sessionError = MutableSharedFlow<Throwable>()
    override val sessionFrame = MutableSharedFlow<SessionFrame>()
    override val conversationStatus: SharedFlow<ConversationState> =
        conversationStateProcessor.conversationStatus
    override val conversationState: StateFlow<ConversationState> =
        conversationStateProcessor.conversationState
    override val interactionHistory: StateFlow<List<InteractionHistoryElement>> =
        conversationStateProcessor.interactionHistory
    override var replayerActive: Boolean = false
    private var recorder: ReplayRecorder? = null

    init {
        conversationStateProcessor.onTimeout = { exception ->
            sessionError.emit(exception)
        }
    }

    override fun postPromptsForStreaming(
        request: MapGptStreamingRequest,
    ) {
        interrupt()
        val query = request.prompt
        val contextJson = jsonParser.encodeToString(request.context)
        conversationStateProcessor.onNewInput(query)
        SharedLog.d(TAG) { "postPromptsForStreaming: replayerActive: $replayerActive query: $query" }
        if (replayerActive) {
            recorder?.recordRequest(MapgptConfiguration.getEndpoint().conversationUrl, request)
        } else {
            platformServiceWrapper.sendQuery(
                query,
                contextJson,
                request.profileId,
                request.capabilities ?: emptySet(),
            )
        }
    }

    override fun connect(
        endpoint: MapGptEndpoint,
        reconnectSessionId: String?,
        sessionType: MapGptSessionType,
    ) {
        connectionJob?.cancel()
        connectionJob = coroutineScope.launch {
            platformServiceWrapper.connect(
                endpoint = endpoint,
                language = languageRepository.language.value,
                profileId = profileId,
                reconnectSessionId = reconnectSessionId,
                mapGptSessionType = sessionType,

                ).collect { state ->
                sessionState.value = state
            }
        }
    }

    override fun disconnect() {
        connectionJob?.cancel()
        coroutineScope.launch {
            mapGptSessionConnectionMutex.withLock {
                conversationStateProcessor.clear()
                conversationStateProcessor.cancel()
                platformServiceWrapper.cancelConnection()
                sessionState.value = SessionState.Disconnected
            }
        }
    }

    override fun interrupt() {
        conversationStateProcessor.cancel()
    }

    override fun onSessionConnectionError(
        nativeSessionConnectionError: NativeSessionConnectionError,
    ) {
        SharedLog.d(TAG) { "onSessionConnectionError: $nativeSessionConnectionError" }
        when (nativeSessionConnectionError) {
            NativeSessionConnectionError.AlreadyConnectedError -> coroutineScope.launch {
                sessionState.value = SessionState.Disconnected
            }.invokeOnCompletion {
                SharedLog.d(TAG) { "onSessionConnectionError. ioc: $it" }
            }

            NativeSessionConnectionError.HttpError,
            NativeSessionConnectionError.InvalidResponseError,
            NativeSessionConnectionError.NotConnectedError,
            NativeSessionConnectionError.WssError,
            NativeSessionConnectionError.WrongSessionTypeError,
            NativeSessionConnectionError.OtherError,
            -> {
                sessionError.tryEmit(nativeSessionConnectionError.toException())
            }
        }
    }

    override fun onError(exception: Exception) {
        SharedLog.d(TAG) { "onError: $exception" }
        coroutineScope.launch {
            sessionError.emit(exception)
        }
    }

    override fun onSessionFrameReceived(sessionFrame: SessionFrame) {
        SharedLog.d(TAG) { "onSessionFrameReceived: $sessionFrame" }
        coroutineScope.launch {
            this@NativeMapGptService.sessionFrame.emit(sessionFrame)
            when (sessionFrame) {
                is SessionFrame.SendEvent ->
                    conversationStateProcessor.updateConversationBuffers(
                        eventBody = sessionFrame.body,
                    )

                else -> {
                    // Do nothing
                }
            }
        }
    }

    override suspend fun replayResponse(rawEvent: String) {
        SharedLog.d(TAG) { "replayResponse: $rawEvent" }
        try {
            if (replayerActive) {
                val event = Result.Success(SessionFrame.fromJsonString(rawEvent))
                sessionFrame.emit(event.value)
                when (event.value) {
                    is SessionFrame.SendEvent -> {
                        conversationStateProcessor.updateConversationBuffers(
                            eventBody = event.value.body,
                        )
                    }

                    else -> {
                        // no-op
                    }
                }
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            SharedLog.d(TAG) { "Exception replaying response ${ex.message}" }
        }
    }

    override fun setReplayRecorder(recorder: ReplayRecorder) {
        this.recorder = recorder
    }

    override fun startAsrRequest(contextDTO: MapGptContextDTO, capabilities: Set<String>) {
        coroutineScope.launch {
            val contextJson = jsonParser.encodeToString(contextDTO)
            platformServiceWrapper.startAsrRequest(
                contextJson = contextJson,
                capabilities = capabilities,
                profile = profileId,
            )
        }
    }

    override fun sendAsrData(data: ByteArray) {
        coroutineScope.launch {
            platformServiceWrapper.sendAsrData(data)
        }
    }

    override fun sendFinalAsrData() {
        coroutineScope.launch {
            platformServiceWrapper.sendFinalAsrData()
        }
    }

    override fun onNewAsrInput(text: String) {
        conversationStateProcessor.onNewInput(text)
    }

    companion object {

        private const val TAG = "NativeMapGptSession"
        private val DEFAULT_PROCESSING_TIMEOUT = 10.seconds
        private val DEFAULT_CANCEL_TIMEOUT = 300.milliseconds
    }
}
