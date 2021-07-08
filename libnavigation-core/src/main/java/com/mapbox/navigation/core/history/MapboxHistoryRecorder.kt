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
    fun fileDirectory(): String? = historyFiles.absolutePath(historyRecorderOptions)

    /**
     * Starts history recording session.
     * If history recording is already started - does nothing.
     * To save history and get a file path call [MapboxHistoryRecorder.stopRecording].
     */
    fun startRecording() {
        checkRecorderInitialized()
        historyRecorderHandle?.apply { startRecording() }
    }

    /**
     * When called, history recording will be stopped and all records will be added to the file.
     * The path to the file will be sent to the [SaveHistoryCallback.onSaved] interface.
     * If history recording was not started, [SaveHistoryCallback] is called with a `null` file
     * path.
     * [MapboxHistoryRecorder.startRecording] can be called before the [SaveHistoryCallback]
     * completes.
     *
     * @see [MapboxHistoryReader] to read the events in the file.
     *
     * @param result Callback which shares the history that has been saved
     *
     */
    fun stopRecording(result: SaveHistoryCallback) {
        checkRecorderInitialized()
        historyRecorderHandle?.apply {
            stopRecording { filePath: String? ->
                result.onSaved(filePath)
            }
        }
    }

    /**
     * Adds a custom event to the navigators history. This can be useful to log things that
     * happen during navigation that are specific to your application.
     *
     * @param eventType the event type in the events log for your custom even
     * @param eventJson the json to attach to the "properties" key of the event
     */
    fun pushHistory(eventType: String, eventJson: String) {
        checkRecorderInitialized()
        historyRecorderHandle?.apply { pushHistory(eventType, eventJson) }
    }

    private fun checkRecorderInitialized() {
        if (historyRecorderHandle == null) {
            logger.w(Tag("MbxHistoryRecorder"), Message("The history recorder is not initialized"))
        }
    }
}

/**
 * Callback which called as a result of [MapboxHistoryRecorder.stopRecording]
 */
fun interface SaveHistoryCallback {
    /**
     * @param filepath is null if [MapboxHistoryRecorder.stopRecording] called without any
     * events received or the navigator is not configured.
     */
    fun onSaved(filepath: String?)
}
