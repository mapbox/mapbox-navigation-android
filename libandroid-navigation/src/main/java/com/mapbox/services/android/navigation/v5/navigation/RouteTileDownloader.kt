package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.services.android.navigation.v5.utils.DownloadTask

/**
 * This class serves to contain the complicated chain of events that must happen to download
 * offline routing tiles. It creates and maintains a directory structure with the root in the
 * Offline directory, or wherever someone specifies.
 */
internal class RouteTileDownloader(
    private val offlineNavigator: OfflineNavigator,
    private val tilePath: String,
    private val listener: RouteTileDownloadListener
) {

    companion object {
        private const val FILE_EXTENSION_TAR = "tar"
    }

    fun startDownload(offlineTiles: OfflineTiles) {
        val version = offlineTiles.version()
        val tarFetchedCallback = buildTarFetchedCallback(version)
        offlineTiles.fetchRouteTiles(tarFetchedCallback)
    }

    fun onError(error: OfflineError) = listener.onError(error)

    private fun buildTarFetchedCallback(version: String): TarFetchedCallback {
        val downloadTask = buildDownloadTask(tilePath, version)
        return TarFetchedCallback(this, downloadTask)
    }

    private fun buildDownloadTask(tilePath: String, tileVersion: String): DownloadTask {
        val tileUnpacker = TileUnpacker(offlineNavigator)
        val downloadListener = DownloadUpdateListener(
            this,
            tileUnpacker,
            tilePath,
            tileVersion,
            listener
        )
        return DownloadTask(
            tilePath,
            tileVersion,
            FILE_EXTENSION_TAR,
            downloadListener
        )
    }
}
