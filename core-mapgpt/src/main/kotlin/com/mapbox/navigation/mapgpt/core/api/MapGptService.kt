package com.mapbox.navigation.mapgpt.core.api

import com.mapbox.navigation.mapgpt.core.MapGptEndpoint
import com.mapbox.navigation.mapgpt.api.native.MapGptSessionType
import com.mapbox.navigation.mapgpt.core.internaltools.ReplayRecorder
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MapGptService {

    /**
     * Provides a state of connection to the MapGPT service. [connect] and [disconnect] can be used to alter this state.
     */
    val sessionState: StateFlow<SessionState>

    /**
     * Surfaces any errors that might've been encountered during the runtime of MapGPT service.
     */
    val sessionError: SharedFlow<Throwable>

    /**
     * Provides a raw stream of frames from the MapGPT service.
     *
     * Events in this stream and chunks associated with these events come without any strict order.
     * If you rely on the order of events, especially for conversation use-cases like playback or display in a chat UI,
     * use [conversationState] and [interactionHistory].
     */
    val sessionFrame: SharedFlow<SessionFrame>

    /**
     * Provides all intermediate values of [ConversationState].
     */
    val conversationStatus: SharedFlow<ConversationState>

    /**
     * Conflates [conversationStatus] into s single state value.
     *
     * This is a [StateFlow] and shouldn't be used if you need to receive status about each ongoing conversation.
     * If your collector is slow, intermediate states might not be delivered, so to track all conversations it's best to pair
     * [conversationStatus] with [interactionHistory].
     *
     * If you need to guarantee access to each conversation as it happens, use [conversationStatus] instead.
     */
    val conversationState: StateFlow<ConversationState>

    /**
     * Provides in-memory history of the previous [postPromptsForStreaming] calls and [BufferedConversation]s within the current session.
     *
     * When a request is made or as soon as a conversation finishes, it's put in this state as the last element.
     *
     * Starting a new session clears this state.
     */
    val interactionHistory: StateFlow<List<InteractionHistoryElement>>


    /**
     * To be invoked when ready to make ASR request using [MapboxUserInputMiddleware]
     *
     * @param contextDTO MapGpt context
     * @param capabilities set of capabilities enabled
     */
    fun startAsrRequest(contextDTO: MapGptContextDTO, capabilities: Set<String>)

    /**
     * To be invoked when ASR data is available to be sent.
     *
     * @param data user input stream
     */
    fun sendAsrData(data: ByteArray)

    /**
     * To be invoked when the final ASR data has been sent.
     */
    fun sendFinalAsrData()

    /**
     * Use to send user input to the backend. Client needs to be connected via [connect] first.
     *
     * This function does not throw and errors are surfaced via [sessionError] instead.
     *
     * The service will monitor the conversation and if there's no output for [processingTimeout] duration,
     * the conversation will be interrupted and a [ConversationTimeoutException] will be delivered
     * to the [sessionError] flow.
     *
     * @param request request parameters
     */
    fun postPromptsForStreaming(
        request: MapGptStreamingRequest,
    )

    /**
     * Connects to a previously opened session, when [reconnectSessionId] is provided, or starts a new session.
     *
     * The state of the connection can be observed via [sessionState],
     * where [SessionState.Connected.sessionId] contains current session ID which can be stored
     * and used for reconnection across app launches.
     *
     * Upon a failed connection attempt, or loss of connection, the service will automatically try to reconnect
     * resulting in a cycle of [SessionState.Connecting] and [SessionState.Connected] states.
     *
     * Only when [SessionState.Disconnected] is set the service will not try to reconnect
     * and a new connection needs to be initiated by calling [connect].
     *
     * The function doesn't throw but might push errors to the [sessionError] while it tries to connect.
     *
     * Only one session can be opened at a time, so calling this function without [reconnectSessionId] will cancel currently active session, if one exists.
     *
     * @param endpoint custom API host
     * @param reconnectSessionId previous session ID to reconnect to
     * @param sessionType session type associated with the connection.
     */
    fun connect(
        endpoint: MapGptEndpoint = MapGptEndpoint.create(),
        reconnectSessionId: String? = null,
        sessionType: MapGptSessionType = MapGptSessionType.BOTH,
    )

    /**
     * Closes the current session and disconnects from the host.
     */
    fun disconnect()

    /**
     * Attempts to cancel stream of frames for the ongoing conversation, if there's any.
     *
     * There might still be frames delivered after this function returns,
     * if they were already being processed by worker threads.
     */
    fun interrupt()

    suspend fun replayResponse(rawEvent: String)

    fun setReplayRecorder(recorder: ReplayRecorder)

    var replayerActive: Boolean
}
