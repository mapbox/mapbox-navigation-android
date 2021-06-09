package com.mapbox.navigation.core.history

import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigator.HistoryRecorderHandle

/**
 * Provides a mechanism for saving and retrieving Mapbox navigation history files.
 * Retrieve an instance of this class through [MapboxNavigation.historyRecorder].
 *
 * @see [HistoryRecorderOptions] to enable the recorder.
 * @see [MapboxHistoryReader] to read the history files.
 */
class MapboxHistoryRecorder internal constructor(
    navigationOptions: NavigationOptions,
    private val logger: Logger
) {
    private val historyRecorderOptions = navigationOptions.historyRecorderOptions
    private val historyFiles = HistoryFiles(navigationOptions.applicationContext, logger)

    internal var historyRecorderHandle: HistoryRecorderHandle? = null

    /**
     * The file directory where the history files are stored.
     * Use this to purge old history files, upload later, or
     * to use them for replay.
     *
     * @see [HistoryRecorderOptions] to customize this location.
     *
     * @return absolute path to the directory with history files.
     *     returns null when there is no file directory
     */
    fun fileDirectory(): String? {
        return if (historyRecorderOptions.enabled) {
            historyFiles.absolutePath(historyRecorderOptions)
        } else {
            null
        }
    }

    /**
     * When called, history recording will be paused and all records will be added to the file.
     * The path to the file will be sent to the [SaveHistoryCallback.onSaved] interface.
     * The next call to to this function will include a new file with new events.
     *
     * @see [MapboxHistoryReader] to read the events in the file.
     *
     * @param result Callback which shares the history that has been saved
     *
     * @throws IllegalStateException when [HistoryRecorderOptions.enabled] is false
     */
    fun saveHistory(result: SaveHistoryCallback) {
        check(historyRecorderOptions.enabled) {
            logger.e(
                Tag("MbxHistoryRecorder"),
                Message("You must enable HistoryRecorderOptions in order to save history")
            )
        }
        historyRecorderHandle?.apply {
            dumpHistory { filePath: String? ->
                result.onSaved(filePath)
            }
        } ?: run {
            logger.w(
                Tag("MbxHistoryRecorder"),
                Message("dumpHistory failed - The history recorder is not initialized")
            )
            result.onSaved(null)
        }
    }
}

/**
 * Callback which called as a result of [MapboxHistoryRecorder.saveHistory]
 */
fun interface SaveHistoryCallback {
    /**
     * @param filepath is null if [MapboxHistoryRecorder.saveHistory] called without any
     * events received or the navigator is not configured.
     */
    fun onSaved(filepath: String?)
}
