package com.mapbox.navigation.core.replay.history

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.replay.ReplayEventStream

/**
 * Lower memory option for reading replay events from a history file. Use the [ReplayHistoryMapper]
 * to support replaying custom events with [ReplayHistoryMapper.pushEventMappers].
 *
 * TODO NN-268 MapboxHistoryReader loads the entire file so the memory optimization does not work.
 *   The issue will be addressed without changes to this interface.
 */
class ReplayHistoryEventStream @VisibleForTesting internal constructor(
    private val historyReader: MapboxHistoryReader,
    private val replayHistoryMapper: ReplayHistoryMapper
) : ReplayEventStream {

    /**
     * Construct an event stream from a file. A default event mapper, any unrecognized events will
     * be dropped. Provide a [replayHistoryMapper] to customize how events are mapped.
     *
     * @param filePath absolute path to a file containing the native history file.
     */
    constructor(filePath: String) : this(
        MapboxHistoryReader(filePath),
        ReplayHistoryMapper.Builder().build()
    )

    /**
     * Construct an event stream from a file with a custom event mapper.
     *
     * @param filePath absolute path to a file containing the native history file.
     * @param replayHistoryMapper for customizing how the events are mapped.
     */
    constructor(filePath: String, replayHistoryMapper: ReplayHistoryMapper) : this(
        MapboxHistoryReader(filePath),
        replayHistoryMapper
    )

    private var next: ReplayEventBase? = null

    init {
        loadNext()
    }

    /**
     * Returns `true` if the iteration has more elements.
     */
    override fun hasNext(): Boolean = next != null

    /**
     * Returns the next element in the iteration.
     *
     * @throws NullPointerException when [hasNext] is false.
     */
    @Throws(NullPointerException::class)
    override fun next(): ReplayEventBase = next!!.also {
        loadNext()
    }

    private fun loadNext() {
        do {
            if (historyReader.hasNext()) {
                next = replayHistoryMapper.mapToReplayEvent(historyReader.next())
            } else {
                next = null
                return
            }
        } while (next == null)
    }
}
