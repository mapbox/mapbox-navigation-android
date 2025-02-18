package com.mapbox.navigation.mapgpt.core.common

/**
 * Singleton used to define interaction events for the MapGPT events service.
 *
 * The responsibility of this class is to define common schemas for interaction events
 * which can be used across the SDK and on different platforms.
 */
object MapGptEvents {
    private val platformEventsService: PlatformEventsService = PlatformEventsService()

    /**
     * Sends an error event with the specified component, status, and description.
     *
     * @param component The component where the error occurred.
     * @param status Status of the component triggered an error.
     * @param description Description of the error.
     */
    fun error(component: String, status: String, description: String) {
        val errorEventParams = ErrorEventParams(component, status, description)
        val errorEvent = platformEventsService.errorEvent(errorEventParams)
        platformEventsService.sendError(errorEvent)
    }

    /**
     * Events related to the graphical avatar on the screen.
     */
    fun avatarTapped() {
        interaction(Component.Avatar, Status.Tapped)
    }

    /**
     * Events related to the chat component.
     *
     * @param status The current status of the chat component.
     * @param locale The locale setting for the chat interaction.
     * @param size The size parameter related to the chat interaction, if applicable.
     */
    fun chat(
        status: Status,
        locale: String? = null,
        size: Long? = null,
    ) {
        interaction(Component.Chat, status) {
            this.locale = locale
            this.size = size
        }
    }


    /**
     * Events related to the wake word component.
     *
     * @param status The current status of the wake word component.
     * @param provider The provider of the wake word service, if any.
     * @param onDevice Indicates if the wake word processing is done on-device.
     * @param locale The locale setting for the wake word interaction.
     * @param size The size parameter related to the wake word interaction, if applicable.
     */
    fun wakeWord(
        status: Status,
        provider: String? = null,
        onDevice: Boolean? = null,
        locale: String? = null,
        size: Long? = null,
    ) {
        interaction(Component.WakeWord, status) {
            this.provider = provider
            this.onDevice = onDevice
            this.locale = locale
            this.size = size
        }
    }

    /**
     * Events related to automatic speech recognition (ASR).
     *
     * @param status The current status of the ASR component.
     * @param locale The locale setting for the ASR interaction.
     * @param size The size parameter related to the ASR interaction, if applicable.
     */
    fun asr(
        status: Status,
        locale: String? = null,
        size: Long? = null
    ) {
        interaction(Component.Asr, status) {
            this.locale = locale
            this.size = size
        }
    }

    /**
     * Events related to Text-to-Speech (TTS).
     *
     * @param status The current status of the TTS component.
     * @param endpoint The endpoint used for TTS, if applicable.
     * @param provider The provider of the TTS service, if any.
     * @param onDevice Indicates if the TTS processing is done on-device.
     * @param locale The locale setting for the TTS interaction.
     * @param size The size parameter related to the TTS interaction, if applicable.
     */
    fun tts(
        status: Status,
        endpoint: String? = null,
        provider: String? = null,
        onDevice: Boolean? = null,
        locale: String? = null,
        size: Long? = null
    ) {
        interaction(Component.Tts, status) {
            this.endpoint = endpoint
            this.provider = provider
            this.onDevice = onDevice
            this.locale = locale
            this.size = size
        }
    }

    /**
     * Events related to Voice Activity Detection (VAD).
     *
     * @param status The current status of the VAD component.
     * @param locale The locale setting for the VAD interaction.
     * @param size The size parameter related to the VAD interaction, if applicable.
     */
    fun vad(
        status: Status,
        locale: String? = null,
        size: Long? = null
    ) {
        interaction(Component.Vad, status) {
            this.locale = locale
            this.size = size
        }
    }

    /**
     * Events related to Acoustic Echo Cancellation (AEC).
     *
     * @param status The current status of the AEC component.
     * @param provider The provider of the AEC service, if any.
     * @param onDevice Indicates if the AEC processing is done on-device.
     * @param locale The locale setting for the AEC interaction.
     * @param size The size parameter related to the AEC interaction, if applicable.
     */
    fun aec(
        status: Status,
        provider: String? = null,
        onDevice: Boolean? = null,
        locale: String? = null,
        size: Long? = null
    ) {
        interaction(Component.Aec, status) {
            this.provider = provider
            this.onDevice = onDevice
            this.locale = locale
            this.size = size
        }
    }

    /**
     * Events related to LLM-produced conversations.
     *
     * @param status The current status of the conversation component.
     * @param locale The locale setting for the conversation interaction.
     * @param size The size parameter related to the conversation interaction, if applicable.
     */
    fun conversation(
        status: Status,
        locale: String? = null,
        size: Long? = null
    ) {
        interaction(Component.Conversation, status) {
            this.locale = locale
            this.size = size
        }
    }

    /**
     * Events related to LLM-produced actions.
     *
     * @param status The current status of the conversation component.
     * @param endpoint The endpoint involved in the action, if applicable.
     * @param provider The provider of the action service, if any.
     * @param onDevice Indicates if the action processing is done on-device.
     * @param locale The locale setting for the action received interaction.
     * @param size The size parameter related to the action received interaction, if applicable.
     */
    fun actionReceived(
        status: Status,
        endpoint: String? = null,
        provider: String? = null,
        onDevice: Boolean? = null,
        locale: String? = null,
        size: Long? = null,
    ) {
        interaction(Component.Conversation, status) {
            this.endpoint = endpoint
            this.provider = provider
            this.onDevice = onDevice
            this.locale = locale
            this.size = size
        }
    }

    /**
     * Events related to user interruptions, when the user interrupts MapGPT via voice or avatar.
     *
     * @param status The current status of the interruption component.
     * @param locale The locale setting for the interruption interaction.
     * @param size The size parameter related to the interruption interaction, if applicable.
     */
    fun interruption(
        status: Status,
        locale: String? = null,
        size: Long? = null
    ) {
        interaction(Component.Interruption, status) {
            this.locale = locale
            this.size = size
        }
    }

    /**
     * All interaction events can be sent using this interaction event builder. Can be used to
     * send new events that are being defined.
     *
     * @param component The component that the interaction event is related to.
     * @param status The current status of the action component.
     * @param metadata Additional metadata for the interaction event.
     */
    fun interaction(
        component: Component,
        status: Status,
        metadata: (InteractionMetadata.() -> Unit)? = null,
    ) {
        val params = InteractionEventParams(component.value, status.value)
        params.metadata = InteractionMetadata().apply { metadata?.invoke(this) }
        SharedLog.d(TAG) {
            "component=${component.value}, status=${status.value}, metadata: ${params.metadata}"
        }
        val event = platformEventsService.interactionEvent(params)
        platformEventsService.sendInteraction(event)
    }

    fun generateTraceId(): String = platformEventsService.generateTraceId()

    private const val TAG = "MapGptEvents"
}

open class Component(val value: String) {
    object Avatar : Component("AVATAR")
    object Chat : Component("CHAT")
    object WakeWord : Component("WAKEWORD")
    /** Automatic Speech Recognition */
    object Asr : Component("ASR")
    /** Text to Speech */
    object Tts : Component("TTS")
    object Interruption : Component("INTERRUPTION")
    /** Voice Activity Detection */
    object Vad : Component("VAD")
    /** Acoustic Echo Cancellation */
    object Aec : Component("AEC")
    object Action : Component("ACTION")
    object Conversation : Component("CONVERSATION")
}

open class Status(val value: String) {
    object Enabled : Status("ENABLED")
    object Disabled : Status("DISABLED")
    object Tapped : Status("TAPPED")
    object Opened : Status("OPENED")
    object Closed : Status("CLOSED")
    object Sent : Status("SENT")
    object Detected : Status("DETECTED")
    object Started : Status("STARTED")
    object Stopped : Status("STOPPED")
    object Finished : Status("FINISHED")
    object Paused : Status("PAUSED")

    override fun toString(): String = value
}
