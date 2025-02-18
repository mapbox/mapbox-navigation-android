package com.mapbox.navigation.mapgpt.core.common

import com.mapbox.mapgpt.experimental.MapgptErrorEvent
import com.mapbox.mapgpt.experimental.MapgptEventsService
import com.mapbox.mapgpt.experimental.MapgptInteractionEvent
import com.mapbox.mapgpt.experimental.MapgptInteractionMetadata
import java.util.UUID

internal class PlatformEventsService() {
    fun generateTraceId(): String {
        return MapgptEventsService.generateTraceId()
    }

    fun flush() {
        MapgptEventsService.flush()
    }

    fun interactionEvent(params: InteractionEventParams): PlatformInteractionEvent {
        val nativeEvent = MapgptInteractionEvent.Builder()
            .created(params.created)
            .traceId(params.traceId)
            .eventId(UUID.randomUUID().toString())
            .parentId(params.parentId)
            .component(params.component)
            .status(params.status)
            .metadata(params.metadata?.let { metadata ->
                MapgptInteractionMetadata.Builder()
                    .endpoint(metadata.endpoint)
                    .provider(metadata.provider)
                    .onDevice(metadata.onDevice)
                    .locale(metadata.locale)
                    .size(metadata.size)
                    .build()
            })
            .build()
        return PlatformInteractionEvent(nativeEvent)
    }

    fun errorEvent(params: ErrorEventParams): PlatformErrorEvent {
        val nativeEvent = MapgptErrorEvent.Builder()
            .created(params.created)
            .traceId(params.traceId)
            .eventId(UUID.randomUUID().toString()) // TODO move to native
            .parentId(params.parentId)
            .component(params.component)
            .status(params.status)
            .description(params.description)
            .endpoint(params.endpoint)
            .provider(params.provider)
            .onDevice(params.onDevice)
            .locale(params.locale)
            .build()
        return PlatformErrorEvent(nativeEvent)
    }

    fun sendInteraction(event: PlatformInteractionEvent) {
        MapgptEventsService.sendInteractionEvent(event.nativeEvent)
    }

    fun sendError(event: PlatformErrorEvent) {
        MapgptEventsService.sendErrorEvent(event.nativeEvent)
    }

    fun sendTurnstile() {
        MapgptEventsService.sendTurnstileEvent()
    }
}

internal data class InteractionEventParams(
    val component: String,
    val status: String,
) {
    var created: String? = null
    var parentId: String? = null
    var traceId: String? = null
    var metadata: InteractionMetadata? = null
}

data class InteractionMetadata(
    var endpoint: String? = null,
    var provider: String? = null,
    var onDevice: Boolean? = null,
    var locale: String? = null,
    var size: Long? = null,
)

internal data class ErrorEventParams(
    var component: String,
    var status: String,
    var description: String,
) {
    var created: String? = null
    var parentId: String? = null
    var traceId: String? = null
    var endpoint: String? = null
    var provider: String? = null
    var onDevice: Boolean? = null
    var locale: String? = null
}

internal class PlatformInteractionEvent(
    val nativeEvent: MapgptInteractionEvent,
)

internal class PlatformErrorEvent(
    val nativeEvent: MapgptErrorEvent,
)
