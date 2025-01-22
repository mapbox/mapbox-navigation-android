package com.mapbox.navigation.core.history

import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventMapper
import com.mapbox.navigator.HistoryReader

/**
 * Allows you to read history files previously saved by [MapboxHistoryRecorder].
 * All files in the [MapboxHistoryRecorder.fileDirectory] can be read with this reader.
 *
 * @param filePath absolute path to a file containing the native history file.
 */
class MapboxHistoryReader(
    val filePath: String,
) : Iterator<HistoryEvent> {

    private val nativeHistoryReader = HistoryReader(filePath)
    private val historyEventMapper = HistoryEventMapper()

    private var hasNext: Boolean = false
    private var next: HistoryEvent? = null

    init {
        hasNext = loadNext()
    }

    /**
     * Returns `true` if the iteration has more elements.
     */
    override fun hasNext(): Boolean = hasNext

    /**
     * Returns the next element in the iteration.
     *
     * @throws NullPointerException when [hasNext] is false
     */
    override fun next(): HistoryEvent = next!!.also {
        hasNext = loadNext()
    }

    private fun loadNext(): Boolean {
        val historyRecord = nativeHistoryReader.next()
        next = if (historyRecord != null) {
            historyEventMapper.map(historyRecord)
        } else {
            null
        }
        return next != null
    }
}
