package com.mapbox.services.android.navigation.v5.navigation

import java.io.File
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class DownloadUpdateListenerTest {

    @Test
    fun onFinishedDownloading_tarIsUnpacked() {
        val tileUnpacker = mock<TileUnpacker>(TileUnpacker::class.java)
        val file = mock<File>(File::class.java)
        val downloadUpdateListener = buildDownloadUpdateListener(tileUnpacker)

        downloadUpdateListener.onFinishedDownloading(file)

        verify(tileUnpacker).unpack(
            any(File::class.java),
            any(String::class.java),
            any<UnpackProgressUpdateListener>(UnpackProgressUpdateListener::class.java)
        )
    }

    @Test
    fun onErrorDownloading_offlineErrorIsSent() {
        val downloader = mock<RouteTileDownloader>(RouteTileDownloader::class.java)
        val downloadUpdateListener = buildDownloadUpdateListener(downloader)

        downloadUpdateListener.onErrorDownloading()

        verify(downloader).onError(any(OfflineError::class.java))
    }

    private fun buildDownloadUpdateListener(tileUnpacker: TileUnpacker): DownloadUpdateListener {
        val downloader = mock<RouteTileDownloader>(RouteTileDownloader::class.java)
        val tilePath = "some/path/"
        val tileVersion = "some-version"
        val listener = mock<RouteTileDownloadListener>(RouteTileDownloadListener::class.java)
        return DownloadUpdateListener(
            downloader, tileUnpacker, tilePath, tileVersion, listener
        )
    }

    private fun buildDownloadUpdateListener(downloader: RouteTileDownloader): DownloadUpdateListener {
        val tileUnpacker = mock<TileUnpacker>(TileUnpacker::class.java)
        val tilePath = "some/path/"
        val tileVersion = "some-version"
        val listener = mock<RouteTileDownloadListener>(RouteTileDownloadListener::class.java)
        return DownloadUpdateListener(
            downloader, tileUnpacker, tilePath, tileVersion, listener
        )
    }
}
