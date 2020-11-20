package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.core.replay.ReplayEventStream

/**
 * This class is responsible for queuing events into the MapboxReplayer. It will take events
 * out of a file and push them into replay.
 *
 * It may be needed to update the [bufferSize] and [batchSize] to support configurations with
 * different requirements. If replay events are large at low frequencies, a small buffer is needed.
 * If replay events are small but frequent, a large buffer is needed. Large events at high
 * frequencies require devices with more memory.
 */
internal class ReplayEventBuffer {
    private val replayEvents = mutableListOf<ReplayEventBase>()
    private var eventStream: ReplayEventStream? = null

    val events: List<ReplayEventBase>
        get() = replayEvents

    /**
     * Threshold defines when there are less than this many events, add more events.
     */
    private var bufferSize: Int = DEFAULT_REPLAY_EVENT_BUFFER_SIZE

    /**
     * Threshold defines the number of events that will be pushed at a time.
     */
    private var batchSize: Int = DEFAULT_REPLAY_EVENT_BATCH_SIZE

    /**
     * Ignore buffer sizes and add a list of events.
     */
    fun pushEvents(events: List<ReplayEventBase>) {
        this.replayEvents.addAll(events)
    }

    /**
     * Close the previous stream, and start pushing new events from the stream.
     *
     * @param eventStream contains a stream of [ReplayEventBase]
     */
    fun attachStream(eventStream: ReplayEventStream?) {
        this.eventStream?.close()
        this.eventStream = eventStream
        bufferEvents()
    }

    /**
     * This needs to be periodically called in order to push new events.
     */
    fun bufferEvents() {
        val queuedEvents = replayEvents.size
        if (queuedEvents < bufferSize && eventStream?.hasNext() == true) {
            eventStream?.asSequence()
                ?.take(batchSize)
                ?.toList()
                ?.let { replayEvents ->
                    pushEvents(replayEvents)
                }
        }
    }

    /**
     * Clear all the events, and close the event stream.
     */
    fun clear() {
        eventStream?.close()
        replayEvents.clear()
    }

    private companion object {
        private const val DEFAULT_REPLAY_EVENT_BUFFER_SIZE = 100
        private const val DEFAULT_REPLAY_EVENT_BATCH_SIZE = 50
    }
}
