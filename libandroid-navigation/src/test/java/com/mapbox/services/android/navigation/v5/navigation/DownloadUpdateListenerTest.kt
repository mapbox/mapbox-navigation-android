package com.mapbox.services.android.navigation.v5.navigation

import io.mockk.mockk
import io.mockk.verify
import java.io.File
import org.junit.Test

class DownloadUpdateListenerTest {

    @Test
    fun onFinishedDownloading_tarIsUnpacked() {
        val tileUnpacker = mockk<TileUnpacker>(relaxed = true)
        val file = mockk<File>(relaxed = true)
        val downloadUpdateListener = buildDownloadUpdateListener(tileUnpacker)

        downloadUpdateListener.onFinishedDownloading(file)

        verify { tileUnpacker.unpack(any(), any(), any()) }
    }

    @Test
    fun onErrorDownloading_offlineErrorIsSent() {
        val downloader = mockk<RouteTileDownloader>(relaxed = true)
        val downloadUpdateListener = buildDownloadUpdateListener(downloader)

        downloadUpdateListener.onErrorDownloading()

        verify { downloader.onError(any()) }
    }

    private fun buildDownloadUpdateListener(tileUnpacker: TileUnpacker): DownloadUpdateListener {
        val downloader = mockk<RouteTileDownloader>(relaxed = true)
        val tilePath = "some/path/"
        val tileVersion = "some-version"
        val listener = mockk<RouteTileDownloadListener>(relaxed = true)
        return DownloadUpdateListener(
            downloader, tileUnpacker, tilePath, tileVersion, listener
        )
    }

    private fun buildDownloadUpdateListener(downloader: RouteTileDownloader): DownloadUpdateListener {
        val tileUnpacker = mockk<TileUnpacker>(relaxed = true)
        val tilePath = "some/path/"
        val tileVersion = "some-version"
        val listener = mockk<RouteTileDownloadListener>(relaxed = true)
        return DownloadUpdateListener(
            downloader, tileUnpacker, tilePath, tileVersion, listener
        )
    }
}
