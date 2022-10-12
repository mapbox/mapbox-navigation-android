package com.mapbox.navigation.core.internal

import androidx.annotation.UiThread

/**
 * Interface definition for an observer that gets notified whenever
 * some actions are required to record each trip session
 * (Free Drive and Active Guidance) independently.
 */
@UiThread
interface HistoryRecordingStateChangeObserver {

    /**
     * Invoked when history recording should be started:
     * ```kotlin
     * override fun onShouldStartRecording(state: HistoryRecordingSessionState) {
     *     mapboxNavigation.historyRecorder.startRecording()
     * }
     * ```
     *
     * @param state session that should be recorded.
     */
    fun onShouldStartRecording(state: HistoryRecordingSessionState)

    /**
     * Invoked when history recording should be stopped. Meaning that the session was valid
     * and contained some events.
     * ```kotlin
     * mapboxNavigation.historyRecorder.stopRecording { filePath ->
     *     if (filePath != null) {
     *         // upload file
     *     }
     * }
     * ```
     *
     * @param state session for which the recording should be stopped.
     */
    fun onShouldStopRecording(state: HistoryRecordingSessionState)

    /**
     * Invoked when history recording should be cancelled. Meaning that the session was empty
     * and did not contain any events.
     * Normally in [onShouldCancelRecording] you should stop the recording and ignore or delete
     * the history file:
     * ```kotlin
     * mapboxNavigation.historyRecorder.stopRecording { filePath ->
     *     if (filePath != null) {
     *         // delete file
     *     }
     * }
     * ```
     *
     * @param state session for which the recording should be cancelled.
     */
    fun onShouldCancelRecording(state: HistoryRecordingSessionState)
}
