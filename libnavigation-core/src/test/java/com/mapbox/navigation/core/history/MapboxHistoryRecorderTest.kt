package com.mapbox.navigation.core.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
class MapboxHistoryRecorderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val navigationOptionsBuilder = NavigationOptions.Builder(context)
    private val logger: Logger = mockk(relaxUnitFun = true)

    @Test
    fun `historyRecorder fileDirectory is null by default`() {
        val navigationOptions = navigationOptionsBuilder.build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, logger)

        val defaultDirectory = historyRecorder.fileDirectory()

        assertTrue(defaultDirectory.isNullOrEmpty())
    }

    @Test
    fun `historyRecorder fileDirectory is valid when enabled`() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(
                HistoryRecorderOptions.Builder()
                    .enabled(true)
                    .build()
            )
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, logger)

        val defaultDirectory = historyRecorder.fileDirectory()!!

        assertTrue(defaultDirectory.endsWith("mbx_nav/history"))
    }

    @Test
    fun `historyRecorder fileDirectory accepts custom file directories`() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(
                HistoryRecorderOptions.Builder()
                    .enabled(true)
                    .fileDirectory("historyRecorder/test")
                    .build()
            )
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, logger)

        val fileDirectory = historyRecorder.fileDirectory()!!

        assertTrue(fileDirectory.endsWith("historyRecorder/test"))
        assertTrue(File(fileDirectory).delete())
    }

    @Test
    fun `historyRecorder saveHistory logs a warning when it is not initialized`() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(
                HistoryRecorderOptions.Builder()
                    .enabled(true)
                    .build()
            )
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, logger)

        val countDownLatch = CountDownLatch(1)
        historyRecorder.saveHistory { filePath ->
            countDownLatch.countDown()
            assertNull(filePath)
        }
        countDownLatch.await()

        verify { logger.w(any(), any()) }
    }
}
