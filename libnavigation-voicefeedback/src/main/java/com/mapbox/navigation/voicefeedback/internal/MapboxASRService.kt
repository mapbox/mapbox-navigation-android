package com.mapbox.navigation.voicefeedback.internal

import android.annotation.SuppressLint
import com.mapbox.common.ValueConverter
import com.mapbox.mapgpt.experimental.MapgptAsrTranscript
import com.mapbox.mapgpt.experimental.MapgptConfiguration
import com.mapbox.mapgpt.experimental.MapgptEndpoint
import com.mapbox.mapgpt.experimental.MapgptEndpointType
import com.mapbox.mapgpt.experimental.MapgptMessage
import com.mapbox.mapgpt.experimental.MapgptMessageAction
import com.mapbox.mapgpt.experimental.MapgptMessageConversation
import com.mapbox.mapgpt.experimental.MapgptMessageEntity
import com.mapbox.mapgpt.experimental.MapgptObserver
import com.mapbox.mapgpt.experimental.MapgptSession
import com.mapbox.mapgpt.experimental.MapgptSessionError
import com.mapbox.mapgpt.experimental.MapgptSessionErrorType
import com.mapbox.mapgpt.experimental.MapgptSessionLanguage
import com.mapbox.mapgpt.experimental.MapgptSessionMode
import com.mapbox.mapgpt.experimental.MapgptSessionOptions
import com.mapbox.mapgpt.experimental.MapgptSessionReconnecting
import com.mapbox.mapgpt.experimental.MapgptSessionType
import com.mapbox.mapgpt.experimental.MapgptStartSession
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.voicefeedback.FeedbackAgentEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ExperimentalPreviewMapboxNavigationAPI
internal interface MapboxASRService {

    fun connect(token: String)
    suspend fun disconnect()
    fun startAsrRequest()
    fun sendFinalAsrData(abort: Boolean)
    fun sendAsrData(data: ByteArray)

    val sessionState: Flow<AsrSessionState>
    val asrData: Flow<AsrData?>
}

@OptIn(ExperimentalTime::class)
@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxASRServiceImpl(
    private val profileId: String = DEFAULT_FEEDBACK_PROFILE_ID,
    private val language: Locale,
    private val endpoint: FeedbackAgentEndpoint,
    private val feedbackAgentContextProvider: FeedbackAgentContextProvider,
    private val cancelTimeout: Duration = DEFAULT_CANCEL_TIMEOUT,
    private val coroutineScope: CoroutineScope = MainScope(),
    private val jsonDecoder: Json = Json { ignoreUnknownKeys = true },
) : MapboxASRService {

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        serializersModule = sessionFrameSerializerModule
        explicitNulls = false
    }

    private val mapgptSession: MapgptSession = MapgptSession()
    private var connectionJob: Job? = null
    override val asrData = MutableSharedFlow<AsrData?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val sessionState = MutableStateFlow<AsrSessionState>(AsrSessionState.Disconnected)
    private var listeningActive = false

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun connect(token: String) {
        connectionJob?.cancel()
        connectionJob = coroutineScope.launch(Dispatchers.Main) {
            connect(
                endpoint = endpoint.toNativeMapGptEndpoint(),
                token = token,
            ).collect { state ->
                sessionState.value = state
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun connect(
        token: String,
        endpoint: MapgptEndpoint,
    ): Flow<AsrSessionState> = callbackFlow {
        logD(TAG) { "connect: $endpoint" }
        MapgptConfiguration.setEndpoint(endpoint)

        logD(TAG) { "Connecting to streamingApiHost ${endpoint.websocketUrlAsr}" }
        val options = MapgptSessionOptions.Builder()
            .token(token)
            .uuid(UUID.randomUUID().toString())
            .type(MapgptSessionType.ASR)
            .mode(MapgptSessionMode.ONLINE)
            .language(
                when (language.language.lowercase(Locale.ROOT)) {
                    "zh" -> MapgptSessionLanguage.CHINESE
                    "nl" -> MapgptSessionLanguage.DUTCH
                    "en" -> MapgptSessionLanguage.ENGLISH
                    "fr" -> MapgptSessionLanguage.FRENCH
                    "de" -> MapgptSessionLanguage.GERMAN
                    "he" -> MapgptSessionLanguage.HEBREW
                    "it" -> MapgptSessionLanguage.ITALIAN
                    "ja" -> MapgptSessionLanguage.JAPANESE
                    "ko" -> MapgptSessionLanguage.KOREAN
                    "es" -> MapgptSessionLanguage.SPANISH
                    else -> MapgptSessionLanguage.ENGLISH
                },
            )
            .profile(profileId)
            .reconnect(true)
            .build()

        val mapGptObserver = MapboxASRObserver(
            onMapGptSessionStartedCallback = { sessionId ->
                logD(TAG) { "onMapGptSessionStartedCallback: $sessionId" }
                trySend(AsrSessionState.Connected(endpoint.websocketUrlAsr, sessionId))
            },
            onMapGptSessionErrorCallback = { nativeError ->
                logD(TAG) { "onMapGptSessionErrorCallback: $nativeError" }
                trySend(AsrSessionState.Disconnected)
            },
            onReconnecting = { reconnectionData ->
                logD(TAG) { "onReconnecting: $reconnectionData" }
                trySend(
                    AsrSessionState.Connecting(
                        apiHost = endpoint.websocketUrlAsr,
                        reconnectSessionId = reconnectionData,
                    ),
                )
            },
            onAsrTranscriptReceived = { text: String, isFinal: Boolean ->
                logD(TAG) { "onAsrTranscriptReceived: $text isFinal: $isFinal" }
                if (listeningActive) asrData.tryEmit(AsrData.Transcript(text, isFinal))
                if (isFinal) listeningActive = false
            },
            onFeedbackReceived = { feedbackDTO ->
                logD(TAG) { "onFeedbackReceived: $feedbackDTO" }
                asrData.tryEmit(
                    AsrData.Result(
                        feedbackDTO.feedbackDescription,
                        feedbackDTO.feedbackType,
                    ),
                )
            },
        )
        mapgptSession.connect(options, mapGptObserver)

        awaitClose {
            mapgptSession.cancelConnection {
                logD(TAG) { "Connection cancelled: $it" }
            }
        }
    }

    override suspend fun disconnect() {
        connectionJob?.cancel()
        withTimeoutOrNull<Unit>(cancelTimeout) {
            suspendCancellableCoroutine { continuation ->
                logD(TAG) { "platform cancelConnection start" }
                mapgptSession.cancelConnection {
                    logD(TAG) { "platform connection cancelled: $it" }
                    continuation.resume(Unit)
                }
                logD(TAG) { "platform cancelConnection complete" }
            }
        }
    }

    override fun startAsrRequest() {
        asrData.tryEmit(null)
        listeningActive = true
        val contextDTO = feedbackAgentContextProvider.getContext()
        val contextJson = jsonParser.encodeToString(contextDTO)
        logD(TAG) { "startAsrRequest called with context = $contextJson" }
        ValueConverter.fromJson(contextJson).onValue { context ->
            mapgptSession.startAsrRequest(context, emptyList(), profileId)
        }.onError { error ->
            logE(TAG) { "Start ASR failed: $error" }
        }
    }

    override fun sendFinalAsrData(abort: Boolean) {
        listeningActive = false
        asrData.tryEmit(null)
        mapgptSession.finalizeAsrRequest(abort)
    }

    override fun sendAsrData(data: ByteArray) {
        mapgptSession.sendAsrData(data)
    }

    @SuppressLint("RestrictedApi")
    private fun String.toMapgptEndpointType(): MapgptEndpointType? {
        return MapgptEndpointType.values().firstOrNull { mapgptEndpointType ->
            mapgptEndpointType.name.equals(this, true)
        }
    }

    private inner class MapboxASRObserver(
        private val onMapGptSessionStartedCallback: (sessionId: String) -> Unit,
        private val onMapGptSessionErrorCallback: (nativeError: MapgptSessionErrorType) -> Unit,
        private val onAsrTranscriptReceived: (text: String, isFinal: Boolean) -> Unit,
        private val onFeedbackReceived: (feedbackDTO: FeedbackDTO) -> Unit,
        private val onReconnecting: (sessionId: String) -> Unit,
    ) : MapgptObserver {

        override fun onMapgptSessionStarted(message: MapgptStartSession) {
            logD(TAG) { "onMapgptSessionStarted: $message" }
            onMapGptSessionStartedCallback(message.sessionId)
        }

        override fun onMapgptSessionReconnecting(reconnecting: MapgptSessionReconnecting) {
            logD(TAG) { "onMapgptSessionReconnecting: $reconnecting" }
            onReconnecting(reconnecting.sessionId)
        }

        override fun onMapgptSessionError(error: MapgptSessionError) {
            logD(TAG) { "onMapgptSessionError: $error" }
            onMapGptSessionErrorCallback(error.type)
        }

        override fun onMapgptMessageReceived(message: MapgptMessage) {
            logD(TAG) { "onMapgptMessageReceived: $message" }
        }

        override fun onMapgptConversationReceived(conversation: MapgptMessageConversation) {
            logD(TAG) { "onMapgptConversationReceived: $conversation" }
        }

        override fun onMapgptEntityReceived(entity: MapgptMessageEntity) {
            logD(TAG) { "onMapgptEntityReceived: $entity" }
        }

        override fun onMapgptActionReceived(action: MapgptMessageAction) {
            logD(TAG) { "onMapgptActionReceived: $action" }
            if (action.type == FEEDBACK_ACTION_TYPE) {
                try {
                    val feedbackDTO =
                        jsonDecoder.decodeFromString(
                            FeedbackDTO.serializer(),
                            action.raw.data.toJson(),
                        )
                    onFeedbackReceived(feedbackDTO)
                } catch (se: SerializationException) {
                    logE(TAG) { "onMapgptActionReceived error: $se" }
                } catch (ise: IllegalStateException) {
                    logE(TAG) { "onMapgptActionReceived error: $ise" }
                }
            }
        }

        override fun onMapgptAsrTranscript(transcript: MapgptAsrTranscript) {
            logD(TAG) { "onMapgptAsrTranscript: $transcript" }
            onAsrTranscriptReceived(transcript.text, transcript.isFinal)
        }
    }

    private fun FeedbackAgentEndpoint.toNativeMapGptEndpoint(): MapgptEndpoint {
        return MapgptEndpoint.Builder()
            .name(this.name)
            .type(this.name.toMapgptEndpointType() ?: MapgptEndpointType.PRODUCTION)
            .conversationUrl("")
            .websocketUrlAsr(this.streamingAsrApiHost)
            .websocketUrlText(this.streamingApiHost)
            .build()
    }

    companion object {

        private const val TAG = "MapboxASRService"
        private const val DEFAULT_FEEDBACK_PROFILE_ID = "feedback"
        private val DEFAULT_CANCEL_TIMEOUT = 300.milliseconds
    }
}
