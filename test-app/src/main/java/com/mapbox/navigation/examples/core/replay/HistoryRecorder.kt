package com.mapbox.navigation.examples.core.replay

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPOutputStream

/**
 * Example showing how to record navigation history and safe it to a file.
 *
 * To access the file in Android Studio
 *  Go to View > Tools Windows > Device File Explorer > data > com.mapbox.navigation.examples > cache > history-cache
 */
class HistoryRecorder(
    private val navigation: MapboxNavigation
) : TripSessionStateObserver {

    private var startedAt: Date? = null

    /**
     * Attach the history recorder to your MapboxNavigation by using
     * [MapboxNavigation.registerTripSessionStateObserver]
     */
    override fun onSessionStateChanged(tripSessionState: TripSessionState) {
        when (tripSessionState) {
            TripSessionState.STARTED -> startRecording()
            TripSessionState.STOPPED -> stopRecording()
        }
    }

    /**
     * Manually start recording history. Make sure to do this after
     * [MapboxNavigation.startTripSession]
     */
    fun startRecording() {
        startedAt = Date()
        navigation.toggleHistory(true)
    }

    /**
     * Manually stop and save the history file.
     */
    fun stopRecording() {
        // Only record history that has been started.
        val startedAt = startedAt ?: return
        this.startedAt = null

        // retrieveHistory on the main thread before the navigator is reset.
        val history = navigation.retrieveHistory()
        navigation.toggleHistory(false)
        if (history == "{}") {
            Timber.e("Your history file is empty")
        } else {
            val filename = createFilename(startedAt)
            writeFile(filename, history)
        }
    }

    private fun writeFile(filename: String, history: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val cacheDirectory = navigation.navigationOptions.applicationContext.cacheDir
            val historyDirectory = File(cacheDirectory, "history-cache")
                .also { it.mkdirs() }
            val file = createTempFile(filename, ".json.gz", historyDirectory)
            file.outputStream().use { fos ->
                GZIPOutputStream(fos).use { gzip ->
                    gzip.write(history.toByteArray())
                }
            }
            Timber.i("History file saved to ${file.absolutePath}")
        }
    }

    private fun createFilename(startedAt: Date): String =
        "${utcFormatter.format(startedAt)}_${utcFormatter.format(Date())}_"

    companion object {
        val utcFormatter = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.US)
            .also { it.timeZone = TimeZone.getTimeZone("UTC") }
    }
}
