package com.mapbox.navigation.core.history

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventMapper
import com.mapbox.navigator.HistoryReader
import com.mapbox.navigator.HistoryReaderInterface

/**
 * Allows you to read history files previously saved by [MapboxHistoryRecorder].
 * All files in the [MapboxHistoryRecorder.fileDirectory] can be read with this reader.
 */
class MapboxHistoryReader @VisibleForTesting internal constructor(
    val filePath: String,
    private val nativeHistoryReader: HistoryReaderInterface,
    private val historyEventMapper: HistoryEventMapper
) : Iterator<HistoryEvent> {

    /**
     * @param filePath absolute path to a file containing the native history file.
     */
    constructor(filePath: String) : this(
        filePath,
        HistoryReader(filePath),
        HistoryEventMapper()
    )

    private var next: HistoryEvent? = null

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
     * @throws NullPointerException when [hasNext] is false
     */
    @Throws(NullPointerException::class)
    override fun next(): HistoryEvent = next!!.also {
        loadNext()
    }

    private fun loadNext() {
        val historyRecord = nativeHistoryReader.next()
        next = if (historyRecord != null) {
            historyEventMapper.map(historyRecord)
        } else {
            null
        }
    }
}
