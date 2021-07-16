package com.mapbox.navigation.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.NavigationFiles.Companion.CACHE_DEFAULT_PATH
import com.mapbox.navigation.core.NavigationFiles.Companion.HISTORY_DEFAULT_PATH
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationFilesTests {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val navigationOptionsBuilder = NavigationOptions.Builder(context)
    private val logger: Logger = mockk(relaxUnitFun = true)
    private val navigationFiles = NavigationFiles(context, logger)

    @Test
    fun `history fileDirectory is default when no options provided`() {
        val options = navigationOptionsBuilder.build()
        val defaultDirectory = navigationFiles.historyAbsolutePath(options)

        assertTrue(defaultDirectory.endsWith(HISTORY_DEFAULT_PATH))
    }

    @Test
    fun `history fileDirectory is valid when custom path provided`() {
        val filePath = "custom/history"
        val options = navigationOptionsBuilder
            .historyRecorderOptions(
                HistoryRecorderOptions.Builder()
                    .fileDirectory(filePath)
                    .build()
            )
            .build()

        val defaultDirectory = navigationFiles.historyAbsolutePath(options)

        assertTrue(defaultDirectory.endsWith(filePath))
    }

    @Test
    fun `cache fileDirectory is default when no options provided`() {
        val options = navigationOptionsBuilder.build()
        val defaultDirectory = navigationFiles.cacheAbsolutePath(options)

        assertTrue(defaultDirectory.endsWith(CACHE_DEFAULT_PATH))
    }

    @Test
    fun `cache fileDirectory is valid when custom path provided`() {
        val filePath = "custom/cache"
        val options = navigationOptionsBuilder
            .routingTilesOptions(
                RoutingTilesOptions.Builder()
                    .filePath(filePath)
                    .build()
            )
            .build()

        val defaultDirectory = navigationFiles.cacheAbsolutePath(options)

        assertTrue(defaultDirectory.endsWith(filePath))
    }
}
