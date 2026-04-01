package com.mapbox.navigation.voicefeedback

import android.os.SystemClock
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.voicefeedback.internal.AsrSessionState
import com.mapbox.navigation.voicefeedback.internal.DefaultContextProvider
import com.mapbox.navigation.voicefeedback.internal.MapboxASRServiceImpl
import com.mapbox.navigation.voicefeedback.internal.MapboxAutomaticSpeechRecognitionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

/**
 * Access point for core functionality of the Feedback Agent SDK, including environment
 * configuration, lifecycle management, and surface capabilities.
 *
 * @param options a set of [FeedbackAgentOptions] used to customize various features
 */
@OptIn(ExperimentalTime::class)
@ExperimentalPreviewMapboxNavigationAPI
class FeedbackAgentSession private constructor(
    val options: FeedbackAgentOptions,
) : MapboxNavigationObserver {
    /**
     * Builder for creating a new instance of [FeedbackAgentSession].
     *
     * @param contextProvider Provides additional contextual data for Feedback Agent.
     */
    class Builder {
        private var options: FeedbackAgentOptions = FeedbackAgentOptions.Builder().build()

        /**
         * @param options a set of [FeedbackAgentOptions] used to customize various features
         */
        fun options(options: FeedbackAgentOptions): Builder = apply { this.options = options }

        /**
         * Build the [FeedbackAgentSession].
         */
        fun build(): FeedbackAgentSession = FeedbackAgentSession(options)
    }

    private val microphone = options.microphone

    private var locationMatcherResult: LocationMatcherResult? = null

    private val contextProvider = DefaultContextProvider(options.language) {
        locationMatcherResult
    }

    private val mapboxASRService = MapboxASRServiceImpl(
        language = options.language,
        endpoint = options.endpoint,
        feedbackAgentContextProvider = contextProvider,
    )

    private val engine = MapboxAutomaticSpeechRecognitionEngine(
        mapboxASRService = mapboxASRService,
        microphone = microphone,
    )

    /**
     * A [StateFlow] representing the current user input state.
     *
     * Observers can collect this flow to reactively respond to changes in
     * the recognition process, such as when it starts listening, detects speech,
     * processes results, or encounters errors. A null value means that the ASR service is not
     * connected.
     */
    val asrState: StateFlow<ASRState?> = engine.state

    private var mapboxNavigation: MapboxNavigation? = null

    private val connectAttemptFlow = MutableStateFlow<Long?>(null)

    private lateinit var coroutineScope: CoroutineScope

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     *
     * @param mapboxNavigation instance that is being attached.
     *
     * @see [MapboxNavigationApp.registerObserver]
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        this.mapboxNavigation = mapboxNavigation
        microphone.onAttached(mapboxNavigation)

        coroutineScope.launch(Dispatchers.IO) {
            connectAttemptFlow.filterNotNull().collectLatest {
                logD(TAG) { "Connect attempt" }
                connect(mapboxNavigation.navigationOptions.accessToken ?: "")
            }
        }

        coroutineScope.launch(Dispatchers.Default) {
            mapboxNavigation
                .flowLocationMatcherResult()
                .collect { locationMatcherResult = it }
        }
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     *
     * @param mapboxNavigation instance that is being detached.
     *
     * @see [MapboxNavigationApp.unregisterObserver]
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        stopListening()
        disconnect()
        microphone.onDetached(mapboxNavigation)
        this.mapboxNavigation = null
        coroutineScope.cancel()
    }

    private suspend fun connect(token: String) = withContext(Dispatchers.IO) {
        // Wait for the service to be disconnected if it's not already
        if (mapboxASRService.sessionState.value !is AsrSessionState.Disconnected) {
            logD(TAG) { "Disconnecting from MapboxASRService before connecting again." }
            engine.disconnect()
            mapboxASRService.sessionState.takeWhile { it !is AsrSessionState.Disconnected }
                .collect()
        }

        // Start the connection
        engine.connect(token)

        // Wait for the connection to be established.
        // Save the session state and emit connection attempts when the session is cleared.
        // Disconnect the service whenever the flow is terminated.
        mapboxASRService.sessionState.onEach { state ->
            if (state is AsrSessionState.Connected) {
                logD(TAG) { "onConnected $state" }
            }
        }.onCompletion {
            logD(TAG) { "Connection flow terminated, disconnect from MapboxASRService" }
            engine.disconnect()
        }.collect()
    }

    /**
     * Starts listening for user input.
     */
    fun startListening() {
        engine.startListening()
    }

    /**
     * Stop user input. If the user mic is open and listening, it should be closed
     */
    fun stopListening() {
        engine.stopListening()
    }

    /**
     * Terminate ongoing conversation manually to close microphone and ignore the last user input.
     */
    fun interruptListening() {
        engine.interruptListening()
    }

    /**
     * Starts a new session.
     *
     * Upon a failed connection attempt, or loss of connection, the service will automatically try
     * to reconnect.
     */
    fun connect() {
        connectAttemptFlow.value = SystemClock.elapsedRealtime()
    }

    /**
     * Closes the current session and disconnects from the host.
     */
    fun disconnect() {
        engine.disconnect()
    }

    private companion object {
        private const val TAG = "FeedbackAgentSession"
    }
}
