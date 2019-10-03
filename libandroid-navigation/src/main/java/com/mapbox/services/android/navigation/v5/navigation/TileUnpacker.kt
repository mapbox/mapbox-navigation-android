package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import java.io.File

internal class TileUnpacker(private val offlineNavigator: OfflineNavigator) {

    /**
     * Unpacks a TAR file at the srcPath into the destination directory.
     *
     * @param src where TAR file is located
     * @param destPath to the destination directory
     * @param updateListener listener to listen for progress updates
     */
    fun unpack(
        src: File,
        destPath: String,
        updateListener: UnpackUpdateTask.ProgressUpdateListener
    ) {
        UnpackerTask(offlineNavigator).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR, src.absolutePath, destPath + File.separator
        )
        UnpackUpdateTask(updateListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, src)
    }
}
