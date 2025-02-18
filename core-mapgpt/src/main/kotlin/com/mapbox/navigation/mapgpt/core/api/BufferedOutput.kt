package com.mapbox.navigation.mapgpt.core.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Represents events associated with a verbal or no verbal AI response.
 */
interface BufferedOutput {
    /**
     * Holds all events associated with this response in an ordered fashion.
     */
    val events: StateFlow<List<SessionFrame.SendEvent.Body>>

    /**
     * Processes the event in the order they were received.
     *
     * @return true if the event was consumed, false otherwise
     */
    fun onNewEvent(
        eventBody: SessionFrame.SendEvent.Body,
    ): Boolean
}
