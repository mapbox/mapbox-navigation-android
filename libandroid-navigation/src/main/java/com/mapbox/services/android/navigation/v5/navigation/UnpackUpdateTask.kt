package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import java.io.File

/**
 * This class is an [AsyncTask] which monitors the unpacking of a TAR file and updates a
 * listener so that the view can show the unpacking progress. It monitors the unpacking by
 * periodically checking the file size, because as it's unpacked, the file size will decrease.
 */
internal class UnpackUpdateTask
/**
 * Creates a new UnpackUpdateTask to update the view via a passed [ProgressUpdateListener].
 *
 * @param progressUpdateListener listener to update
 */
    (private val progressUpdateListener: ProgressUpdateListener?) : AsyncTask<File, Long, File>() {

    override fun doInBackground(vararg files: File): File {
        // As the data is unpacked from the file, the file is truncated
        // We are finished unpacking the data when the file is fully 0 bytes
        val tilePack = files[0]
        val size = tilePack.length().toDouble()
        var progress: Long
        do {
            progress = (100.0 * (1.0 - tilePack.length() / size)).toLong()
            publishProgress(progress)
        } while (progress < 100L)

        return tilePack
    }

    override fun onProgressUpdate(vararg values: Long?) {
        progressUpdateListener?.onProgressUpdate(values.getOrNull(0) ?: 0)
    }

    override fun onPostExecute(file: File) {
        super.onPostExecute(file)
        progressUpdateListener?.onCompletion()
    }

    /**
     * Interface to allow view to receive updates about the progress of a file unpacking.
     */
    interface ProgressUpdateListener {
        fun onProgressUpdate(progress: Long)

        fun onCompletion()
    }
}
