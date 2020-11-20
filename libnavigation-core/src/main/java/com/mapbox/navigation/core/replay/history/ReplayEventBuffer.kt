package com.mapbox.navigation.core.replay.history

/**
 * This class is responsible for queuing events into the MapboxReplayer. It will take events
 * out of a file and push them into replay. This creates a lower memory implementation
 * for replaying history files.
 *
 * @param minQueueSize when there are less this many events, add more events
 * @param batchSize when more events are pushed, push up to this many events
 */
internal class ReplayEventBuffer(
    private val minQueueSize: Int = MIN_QUEUE_SIZE,
    private val batchSize: Int = BUFFER_SIZE
) {
    private val replayEvents = mutableListOf<ReplayEventBase>()
    private var eventStream: ReplayEventStream? = null

    val events: List<ReplayEventBase>
        get() = replayEvents

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
        if (queuedEvents < minQueueSize && eventStream?.hasNext() == true) {
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
        private const val MIN_QUEUE_SIZE = 50
        private const val BUFFER_SIZE = 100
    }
}
