package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.services.android.navigation.v5.utils.DownloadTask
import java.io.File

/**
 * Triggers a [TileUnpacker] to unpack the tar file into routing tiles once the FILE_EXTENSION_TAR
 * download is complete.
 */
internal class DownloadUpdateListener(
    private val downloader: RouteTileDownloader,
    private val tileUnpacker: TileUnpacker,
    tilePath: String,
    tileVersion: String,
    private val listener: RouteTileDownloadListener
) : DownloadTask.DownloadListener {

    private val destinationPath: String

    companion object {
        private const val DOWNLOAD_ERROR_MESSAGE =
            "Error occurred downloading tiles: null file found"
    }

    init {
        destinationPath = buildDestinationPath(tilePath, tileVersion)
    }

    override fun onFinishedDownloading(file: File) {
        tileUnpacker.unpack(file, destinationPath, UnpackProgressUpdateListener(listener))
    }

    override fun onErrorDownloading() {
        val error = OfflineError(DOWNLOAD_ERROR_MESSAGE)
        downloader.onError(error)
    }

    private fun buildDestinationPath(tilePath: String, tileVersion: String): String {
        val destination = File(tilePath, tileVersion)
        if (!destination.exists()) {
            destination.mkdirs()
        }
        return destination.absolutePath
    }
}
