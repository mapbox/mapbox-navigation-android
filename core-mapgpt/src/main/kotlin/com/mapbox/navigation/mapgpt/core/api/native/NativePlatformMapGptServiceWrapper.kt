package com.mapbox.navigation.mapgpt.core.api.native

import android.annotation.SuppressLint
import android.util.Log
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
import com.mapbox.mapgpt.experimental.MapgptStartSession
import com.mapbox.navigation.mapgpt.api.native.MapGptSessionType
import com.mapbox.navigation.mapgpt.core.MapGptEndpoint
import com.mapbox.navigation.mapgpt.core.api.MapboxUserInputMiddleware
import com.mapbox.navigation.mapgpt.core.api.SessionFrame
import com.mapbox.navigation.mapgpt.core.api.SessionState
import com.mapbox.navigation.mapgpt.core.api.common.ConvertMapGptMessage
import com.mapbox.navigation.mapgpt.core.api.sessionFrameSerializerModule
import com.mapbox.navigation.mapgpt.core.common.MapGptEvents
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.userinput.UserInputMiddlewareManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.time.Duration

@SuppressLint("LongLogTag")
class NativePlatformMapGptServiceWrapper(
    val middlewareManager: UserInputMiddlewareManager,
    val observer: NativePlatformMapGptObserver,
    val cancelTimeout: Duration,
) {

    private val mapgptSession: MapgptSession = MapgptSession()

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        serializersModule = sessionFrameSerializerModule
        explicitNulls = false
    }

    fun sendQuery(
        query: String,
        contextJson: String,
        profileId: String?,
        capabilities: Set<String>,
    ) {
        Log.d(TAG, "sendQuery query: $query, capabilities: $capabilities")
        ValueConverter.fromJson(contextJson)
            .onValue { context ->
                val traceId = MapGptEvents.generateTraceId()
                mapgptSession.say(
                    /* text = */ query,
                    /* context = */ context,
                    /* capabilities = */ capabilities.toList(),
                    /* profile = */ profileId,
                    /* traceId = */ traceId,
                )
            }
            .onError { error ->
                observer.onError(
                    RuntimeException("Parsing of context $contextJson failed with error: $error"),
                )
            }
    }

    suspend fun connect(
        endpoint: MapGptEndpoint,
        language: Language,
        profileId: String,
        reconnectSessionId: String?,
        mapGptSessionType: MapGptSessionType,
    ): Flow<SessionState> = callbackFlow {

        MapgptConfiguration.setEndpoint(endpoint.toNativeMapgptEndpoint())

        Log.d(TAG, "Connecting to streamingApiHost ${endpoint.streamingApiHost}")
        val options = MapgptSessionOptions.Builder()
            .uuid(reconnectSessionId)
            .type(mapGptSessionType.toMapgptSessionType())
            .mode(MapgptSessionMode.ONLINE)
            .language(
                when (language.locale.language.lowercase(Locale.ROOT)) {
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

        val mapGptObserver = MapGptObserverImpl(
            onMapGptSessionStartedCallback = { sessionId ->
                Log.d(TAG, "onMapGptSessionStartedCallback: $sessionId")
                trySend(SessionState.Connected(endpoint.streamingApiHost, sessionId))
            },
            onMapGptSessionErrorCallback = { nativeError ->
                Log.d(TAG, "onMapGptSessionErrorCallback: $nativeError")
                trySend(SessionState.Disconnected)
                return@MapGptObserverImpl isActive
            },
            onReconnecting = { reconnectionData ->
                Log.d(TAG, "onReconnecting: $reconnectionData")
                trySend(
                    SessionState.Connecting(
                        apiHost = endpoint.streamingApiHost,
                        reconnectSessionId = reconnectionData.sessionId,
                    ),
                )
            },
        )
        mapgptSession.connect(options, mapGptObserver)

        awaitClose {
            mapgptSession.cancelConnection {
                Log.d(TAG, "Connection cancelled: $it")
            }
        }
    }

    suspend fun cancelConnection() {
        // TODO: will be fixed in https://mapbox.atlassian.net/browse/NAVAND-3433
        withTimeoutOrNull(cancelTimeout) {
            suspendCancellableCoroutine { continuation ->
                Log.d(TAG, "platform cancelConnection start")
                mapgptSession.cancelConnection {
                    Log.d(TAG, "platform connection cancelled: $it")
                    continuation.resume(Unit)
                }
                Log.d(TAG, "platform cancelConnection complete")
            }
        }
    }

    suspend fun startAsrRequest(
        contextJson: String,
        capabilities: Set<String>,
        profile: String?,
    ) {
        Log.d(TAG, "startAsrRequest called with capabilities = $capabilities and context = $contextJson")
        ValueConverter.fromJson(contextJson)
            .onValue { context ->
                val traceId = MapGptEvents.generateTraceId()
                mapgptSession.startAsrRequest(context, capabilities.toList(), profile, traceId)
            }
            .onError { error ->
                observer.onError(
                    RuntimeException("Parsing of context $contextJson failed with error: $error"),
                )
            }
    }

    suspend fun sendAsrData(data: ByteArray) {
        Log.d(TAG, "sendAsrData with data")
        mapgptSession.sendAsrData(data)
    }

    suspend fun sendFinalAsrData() {
        Log.d(TAG, "sendFinalAsrData")
        mapgptSession.finalizeAsrRequest(true)
    }

    private inner class MapGptObserverImpl(
        private val onMapGptSessionStartedCallback: (sessionId: String) -> Unit,
        private val onMapGptSessionErrorCallback: (nativeError: NativeSessionConnectionError) -> Boolean,
        private val onReconnecting: (ReconnectionData) -> Unit,
    ) : MapgptObserver {

        override fun onMapgptSessionStarted(message: MapgptStartSession) {
            Log.d(TAG, "onMapgptSessionStarted: $message")
            onMapGptSessionStartedCallback(message.sessionId)
        }

        override fun onMapgptSessionReconnecting(reconnecting: MapgptSessionReconnecting) {
            Log.d(TAG, "onMapgptSessionReconnecting: $reconnecting")
            onReconnecting(
                ReconnectionData(
                    sessionId = reconnecting.sessionId,
                    reason = reconnecting.reason,
                ),
            )
        }

        override fun onMapgptSessionError(error: MapgptSessionError) {
            Log.d(TAG, "onMapgptSessionError: $error")
            val nativeError = when (error.type) {
                MapgptSessionErrorType.NOT_CONNECTED_ERROR -> NativeSessionConnectionError.NotConnectedError
                MapgptSessionErrorType.ALREADY_CONNECTED_ERROR -> NativeSessionConnectionError.AlreadyConnectedError
                MapgptSessionErrorType.HTTP_ERROR -> NativeSessionConnectionError.HttpError
                MapgptSessionErrorType.WSS_ERROR -> NativeSessionConnectionError.WssError
                MapgptSessionErrorType.INVALID_RESPONSE_ERROR -> NativeSessionConnectionError.InvalidResponseError
                MapgptSessionErrorType.WRONG_SESSION_TYPE_ERROR -> NativeSessionConnectionError.WrongSessionTypeError
                MapgptSessionErrorType.OTHER_ERROR -> NativeSessionConnectionError.OtherError
            }
            if (!onMapGptSessionErrorCallback(nativeError)) {
                Log.d(TAG, "onMapgptSessionError send to observer: $error")
                observer.onSessionConnectionError(nativeError)
            }
        }

        override fun onMapgptMessageReceived(message: MapgptMessage) {
            Log.d(TAG, "onMapgptMessageReceived: $message")
            try {
                val sharedMessage = ConvertMapGptMessage(
                    action = SEND_EVENT_SESSION_FRAME_ACTION,
                    body = ConvertMapGptMessage.Body(
                        id = message.id,
                        timestamp = message.timestamp,
                        isSupplement = message.isSupplement,
                        chunkId = message.chunkId,
                        type = message.type,
                        data = jsonParser.parseToJsonElement(message.data.toJson()),
                    ),
                )
                val frame = SessionFrame.fromJsonString(jsonParser.encodeToString(sharedMessage))
                observer.onSessionFrameReceived(frame)
            } catch (exception: Exception) {
                observer.onError(exception)
            }
        }

        override fun onMapgptConversationReceived(conversation: MapgptMessageConversation) {
            Log.d(TAG, "onMapgptConversationReceived: $conversation")
        }

        override fun onMapgptEntityReceived(entity: MapgptMessageEntity) {
            Log.d(TAG, "onMapgptEntityReceived: $entity")
        }

        override fun onMapgptActionReceived(action: MapgptMessageAction) {
            Log.d(TAG, "onMapgptActionReceived: $action")
        }

        override fun onMapgptAsrTranscript(transcript: MapgptAsrTranscript) {
            Log.d(TAG, "onMapgptAsrTranscript: $transcript")
            val middleware = middlewareManager.userInputMiddleware.value as? MapboxUserInputMiddleware
            middleware?.onTranscriptReceived(
                transcript = transcript.text,
                isFinal = transcript.isFinal,
            ) ?: Log.d(TAG,"onMapgptAsrTranscript: $middleware is not an instance of " +
                "MapboxUserInputMiddleware")
            if (transcript.isFinal) {
                observer.onNewAsrInput(transcript.text)
            }
        }
    }
    @SuppressLint("RestrictedApi")
    private fun String.toMapgptEndpointType(): MapgptEndpointType? {
        return MapgptEndpointType.values().firstOrNull { mapgptEndpointType ->
            mapgptEndpointType.name.equals(this,true)
        }
    }

    private fun String.formatIfNeed(): String =
        if (this.startsWith(HTTPS_PREFIX)) this else "$HTTPS_PREFIX$this"

    @SuppressLint("RestrictedApi")
    private fun MapGptEndpoint.toNativeMapgptEndpoint(): MapgptEndpoint {
        return MapgptEndpoint.Builder()
            .name(this.name)
            .type(this.name.toMapgptEndpointType() ?: MapgptEndpointType.PRODUCTION)
            .conversationUrl(this.conversationApiHost.formatIfNeed())
            .websocketUrlAsr(this.streamingAsrApiHost)
            .websocketUrlText(this.streamingApiHost)
            .build()
    }

    companion object {
        private const val TAG = "NativeMapGptSessionWrapper"
        private const val SEND_EVENT_SESSION_FRAME_ACTION = "send-event"
        private const val HTTPS_PREFIX = "https://"
    }
}
