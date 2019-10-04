package com.mapbox.services.android.navigation.v5.navigation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class RouteTileDownloaderTest {

    @Test
    fun startDownload_fetchRouteTilesIsCalled() {
        val tilePath = "some/path/"
        val offlineNavigator = mockk<OfflineNavigator>()
        val listener = mockk<RouteTileDownloadListener>()
        val offlineTiles = mockk<OfflineTiles>(relaxed = true)

        every { offlineTiles.version() } returns "some-version"

        val downloader = RouteTileDownloader(offlineNavigator, tilePath, listener)
        downloader.startDownload(offlineTiles)

        verify { offlineTiles.fetchRouteTiles(any()) }
    }

    @Test
    fun onError_downloadListenerErrorTriggered() {
        val tilePath = "some/path/"
        val offlineNavigator = mockk<OfflineNavigator>()
        val listener = mockk<RouteTileDownloadListener>(relaxed = true)
        val offlineError = mockk<OfflineError>()
        val downloader = RouteTileDownloader(offlineNavigator, tilePath, listener)

        downloader.onError(offlineError)

        verify { listener.onError(offlineError) }
    }
}
