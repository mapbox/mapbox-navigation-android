package com.mapbox.services.android.navigation.v5.navigation

/**
 * Updates any UI elements on the status of the TAR unpacking.
 */
internal class UnpackProgressUpdateListener(private val listener: RouteTileDownloadListener) :
    UnpackUpdateTask.ProgressUpdateListener {

    override fun onProgressUpdate(progress: Long) {
        listener.onProgressUpdate(progress.toInt())
    }

    override fun onCompletion() {
        listener.onCompletion()
    }
}
