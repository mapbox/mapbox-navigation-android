package com.mapbox.navigation.mapgpt.core.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a state of actions requiring no verbal response from the backend.
 */
class BufferedNoResponse internal constructor(
    event: SessionFrame.SendEvent.Body.NoResponse,
) : BufferedOutput {
    private val _events = MutableStateFlow<List<SessionFrame.SendEvent.Body>>(emptyList())

    /**
     * Holds all events associated with this conversation in an ordered fashion.
     */
    override val events = _events.asStateFlow()

    val chunkPrefix = event.chunkPrefix

    init {
        onNewEvent(event)
    }

    override fun onNewEvent(
        eventBody: SessionFrame.SendEvent.Body,
    ): Boolean {
        return when {
            eventBody is SessionFrame.SendEvent.Body.NoResponse -> {
                eventBody.chunkPrefix == chunkPrefix
            }
            eventBody.chunkPrefix == chunkPrefix -> {
                val update = events.value.plus(eventBody)
                _events.value = update
                true
            }
            else -> {
                false
            }
        }
    }
}
