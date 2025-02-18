package com.mapbox.navigation.mapgpt.core.analytics

import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotCarSpeed
import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotDrivingStyle
import com.mapbox.navigation.mapgpt.core.common.MapGptEvents
import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotLaneDirection
import com.mapbox.navigation.mapgpt.core.api.climate.Occupant
import com.mapbox.navigation.mapgpt.core.api.window.WindowPosition
import com.mapbox.navigation.mapgpt.core.common.Status
import com.mapbox.navigation.mapgpt.core.wakeword.WakeWordProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Logs analytics events for MapGPT. This interface serves as a relay for [MapGptAnalyticsEvent].
 * Use the [events] or [MapGptAnalyticsEventObserver] to track events to services like Firebase or
 * other destinations like files or consoles.
 */
interface MapGptAnalytics {

    /**
     * Observable stream of [MapGptAnalyticsEvent]s.
     */
    val events: SharedFlow<MapGptAnalyticsEvent>

    /**
     * Register an observer to receive [MapGptAnalyticsEvent]s. The observer will be notified of
     * all events that are logged through [logEvent]. Consider using [events] instead.
     */
    fun registerObserver(observer: MapGptAnalyticsEventObserver)

    /**
     * Unregister an observer that was registered through [registerObserver]. If the observer was
     * not registered, this method does nothing.
     */
    fun unregisterObserver(observer: MapGptAnalyticsEventObserver)

    /**
     * Log an event.
     */
    fun logEvent(event: MapGptAnalyticsEvent)

    /**
     * Log events with a shorter simpler form.
     *
     * ```kotlin
     * analytics.logEvent(name: "custom_event")
     *     param("leg_index", 2)
     *     param("duration", 4.3)
     *     param("session_id", "abc")
     * }
     * ```
     *
     * ```swift
     * analytics.logEvent(name: "custom_event") { builder in
     *     builder.param(key: "leg_index", valueLong: 2)
     *     builder.param(key: "duration", valueDouble: 4.3)
     *     builder.param(key: "session_id", valueString: "abc")
     * }
     * ```
     */
    fun logEvent(
        name: String,
        block: MapGptAnalyticsEvent.Builder.() -> Unit = {},
    ) {
        logEvent(MapGptAnalyticsEvent.Builder(name).apply(block).build())
    }

    /**
     * Assistant activated by pressing avatar or mic button.
     */
    fun logAssistantActivatedPress() {
        MapGptEvents.avatarTapped()
        logEvent(MapGptAssistantActivatedEvent("press"))
    }

    /**
     * Assistant activated by saying the wake word.
     */
    fun logAssistantActivatedWakeWord(provider: WakeWordProvider) {
        MapGptEvents.wakeWord(Status.Detected, provider = provider.key)
        logEvent(MapGptAssistantActivatedWakeWordEvent(provider))
    }

    /**
     * Assistant activated through bluetooth.
     */
    fun logAssistantActivatedBluetooth() =
        logEvent(MapGptAssistantActivatedEvent("bluetooth"))

    /**
     * Assistant has stopped listening for user input.
     */
    fun logAssistantStoppedListening() =
        logEvent(MapGptAssistantStoppedListeningEvent())

    /**
     * Assistant started replying by voice.
     */
    fun logAssistantReplyVoice() =
        logEvent(MapGptAssistantReplyVoiceEvent())

    fun logAssistantFinishReplyVoice(assistantResponse: String?) =
        logEvent(MapGptAssistantFinishReplyVoiceEvent(assistantResponse ?: ""))

    /**
     * User started talking to assistant.
     */
    fun logUserStartedTalking() =
        logEvent(MapGptUserStartedTalkingEvent())

    /**
     * User stopped talking to assistant.
     */
    fun logUserStoppedTalking(userQuery: String) =
        logEvent(MapGptUserStoppedTalkingEvent(userQuery))

    /**
     * Assistant activated while talking by voice.
     */
    fun logAssistantActivatedWhileTalking() =
        logEvent(MapGptAssistantActivatedWhileTalkingEvent())

    /**
     * Assistant interrupted pressing the avatar while talking.
     */
    fun logAssistantInterruptedPress() =
        logEvent(MapGptAssistantInterruptedPressEvent())

    /**
     * Assistant started replying in chat.
     */
    fun logChatAssistantReply(assistantResponse: String) =
        logEvent(MapGptChatAssistantReplyEvent(assistantResponse))

    /**
     * Chat window opened (keyboard button pressed).
     */
    fun logChatWindowOpened() =
        logEvent(MapGptChatWindowOpenedEvent())

    /**
     * Chat window closed.
     */
    fun logChatWindowClosed() =
        logEvent(MapGptChatWindowClosedEvent())

    /**
     * User sent a message in chat.
     */
    fun logChatUserMessageSent(userQuery: String) =
        logEvent(MapGptChatUserMessageSentEvent(userQuery))

    /**
     * POI and address search activated by voice.
     */
    fun logPoiSearchVoice() =
        logEvent(MapGptPoiSearchVoiceEvent())

    /**
     * Navigation to a particular point activated by voice.
     */
    fun logNavigationActivation() =
        logEvent(MapGptNavigationActivationEvent())

    /**
     * Navigation cancelled by user voice input
     */
    fun logNavigationCancelled() =
        logEvent(MapGptNavigationCancelledEvent())

    /**
     * The assistant has triggered an event to start playing music.
     */
    fun logActionPlayMusic(trackId: String) =
        logEvent(MapGptActionPlayMusicEvent(trackId))

    /**
     * The assistant has triggered an event to pause the current music.
     */
    fun logActionPauseMusic() =
        logEvent(MapGptActionPauseMusicEvent())

    /**
     * The assistant has triggered an event to resume the last played track.
     */
    fun logActionResumeMusic() =
        logEvent(MapGptActionResumeMusicEvent())

    /**
     * The assistant has triggered an event to set the hvac temperature.
     */
    fun logActionSetTemperature(unit: String, previousTemperature: Float, currentTemperature: Float) =
        logEvent(
            MapGptActionSetTemperatureEvent(
                unit = unit,
                previousTemperature = previousTemperature,
                currentTemperature = currentTemperature,
            )
        )

    fun logActionSetAc(acTurnOn: Boolean) =
        logEvent(MapGptActionSetAcEvent(acTurnOn = acTurnOn))

    fun logActionSetAutoClimate(autoClimateTurnOn: Boolean) =
        logEvent(MapGptActionSetAutoClimateEvent(autoClimateTurnOn = autoClimateTurnOn))

    fun logActionSetDefog(defogTurnOn: Boolean) =
        logEvent(MapGptActionSetDefogEvent(defogTurnOn = defogTurnOn))

    fun logActionSetDefrost(defrostTurnOn: Boolean) =
        logEvent(MapGptActionSetDefrostEvent(defrostTurnOn = defrostTurnOn))

    fun logActionSetWindow(occupant: Occupant, position: WindowPosition) =
        logEvent(MapGptActionSetWindowEvent(occupant = occupant, position = position))

    fun logActionLockWindow(lockWindow: Boolean) =
        logEvent(MapGptActionLockWindowEvent(lockWindow = lockWindow))

    /**
     * The assistant will play a local or remote voice announcement.
     */
    fun logPlayVoiceAnnouncement(voice: String) =
        logEvent(MapGptPlayVoiceAnnouncementEvent(voice = voice))

    /**
     * MapGPT service session error.
     */
    fun logServiceSessionError(
        error: String,
        sessionId: String,
        endpointType: String,
        languageTag: String,
    ) = logEvent(
        MapGptServiceSessionError(error, sessionId, endpointType, languageTag),
    )

    /**
     * Elapsed time in millis of first interaction.
     */
    fun logLatencyOfFirstInteraction(
        elapsedTimeInMillis: Long,
        sessionId: String,
        endpointType: String,
        languageTag: String,
    ) = logEvent(
        MapGptLatencyOfFirstInteraction(elapsedTimeInMillis, sessionId, endpointType, languageTag),
    )

    /**
     * The assistant has triggered an event to set the speed.
     */
    fun logActionSetSpeed(speed: Int, unit: String) =
        logEvent(MapGptActionSetSpeedEvent(speed = speed, unit = unit))

    /**
     * The assistant has triggered an event to adjust the speed.
     */
    fun logActionAdjustSpeed(carSpeed: AutopilotCarSpeed, delta: Int?, unit: String) =
        logEvent(MapGptActionAdjustSpeedEvent(carSpeed = carSpeed, delta = delta, unit = unit))

    /**
     * The assistant has triggered an event to set the driving style.
     */
    fun logActionSetDrivingStyle(drivingStyle: AutopilotDrivingStyle) =
        logEvent(MapGptActionSetDrivingStyleEvent(drivingStyle = drivingStyle))

    /**
     * The assistant has triggered an event to change the lane direction.
     */
    fun logActionChangeLane(laneDirection: AutopilotLaneDirection, laneCount: Int) =
        logEvent(MapGptActionChangeLaneEvent(laneDirection = laneDirection, laneCount = laneCount))
}

class MapGptAnalyticsImpl : MapGptAnalytics {

    private val observers = mutableSetOf<MapGptAnalyticsEventObserver>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<MapGptAnalyticsEvent>()
    override val events = _events.asSharedFlow()

    override fun registerObserver(observer: MapGptAnalyticsEventObserver) {
        observers.add(observer)
    }

    override fun unregisterObserver(observer: MapGptAnalyticsEventObserver) {
        observers.remove(observer)
    }

    override fun logEvent(event: MapGptAnalyticsEvent) {
        scope.launch {
            _events.emit(event)
            observers.forEach { it.onEvent(event) }
        }
    }
}
