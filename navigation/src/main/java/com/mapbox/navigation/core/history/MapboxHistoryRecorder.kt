package com.mapbox.navigation.core.history

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.HistoryRecordingEnabledObserver
import com.mapbox.navigation.core.internal.history.HistoryFiles
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigator.HistoryRecorderHandleInterface
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Provides a mechanism for saving and retrieving Mapbox navigation history files.
 * Retrieve an instance of this class through [MapboxNavigation.historyRecorder].
 *
 * @see [HistoryRecorderOptions] to enable the recorder.
 * @see [MapboxHistoryReader] to read the history files.
 */
class MapboxHistoryRecorder internal constructor(
    navigationOptions: NavigationOptions,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    var historyRecorderHandle: HistoryRecorderHandleInterface? = null,
) {

    private var enabled: Boolean = false
        set(value) {
            if (value != field) {
                enabledObservers.forEach { notifyEnabledObserver(value, it) }
                field = value
            }
        }
    private val historyRecorderOptions = navigationOptions.historyRecorderOptions
    private val historyFiles = HistoryFiles(navigationOptions.applicationContext)
    private val enabledObservers = CopyOnWriteArrayList<HistoryRecordingEnabledObserver>()

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
    fun fileDirectory(): String? = historyFiles.historyRecorderAbsolutePath(historyRecorderOptions)

    /**
     * Starts history recording session.
     * If history recording is already started - does nothing.
     * To save history and get a file path call [MapboxHistoryRecorder.stopRecording].
     * @return A list of file paths in which the history recording session will be written
     */
    fun startRecording(): List<String> {
        checkRecorderInitialized()
        enabled = true
        return historyRecorderHandle!!.startRecording()
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
        enabled = false
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

    internal fun copilotFileDirectory(): String? = historyFiles.copilotAbsolutePath()

    internal fun registerHistoryRecordingEnabledObserver(
        observer: HistoryRecordingEnabledObserver,
    ) {
        enabledObservers.add(observer)
        notifyEnabledObserver(enabled, observer)
    }

    internal fun unregisterHistoryRecordingEnabledObserver(
        observer: HistoryRecordingEnabledObserver,
    ) {
        enabledObservers.remove(observer)
    }

    internal fun unregisterAllHistoryRecordingEnabledObservers() {
        enabledObservers.clear()
    }

    private fun checkRecorderInitialized() {
        if (historyRecorderHandle == null) {
            logW("The history recorder is not initialized", "MapboxHistoryRecorder")
        }
    }

    private fun notifyEnabledObserver(enabled: Boolean, observer: HistoryRecordingEnabledObserver) {
        if (enabled) {
            observer.onEnabled(this)
        } else {
            observer.onDisabled(this)
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
