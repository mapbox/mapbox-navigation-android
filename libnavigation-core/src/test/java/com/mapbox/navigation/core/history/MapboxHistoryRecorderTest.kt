package com.mapbox.navigation.core.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MapboxHistoryRecorderTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val navigationOptionsBuilder = NavigationOptions.Builder(context)

    @Before
    fun setup() {
        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(any()) } returns mockk()
    }

    @Test
    fun `historyRecorder fileDirectory is default when no options provided`() {
        val navigationOptions = navigationOptionsBuilder.build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions)

        val defaultDirectory = historyRecorder.fileDirectory()!!

        assertTrue(defaultDirectory.endsWith("mbx_nav/history"))
    }

    @Test
    fun `historyRecorder fileDirectory is valid when custom path provided`() {
        val filePath = "custom/history"
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(
                HistoryRecorderOptions.Builder()
                    .fileDirectory(filePath)
                    .build()
            )
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions)

        val defaultDirectory = historyRecorder.fileDirectory()!!

        assertTrue(defaultDirectory.endsWith(filePath))
    }

    @Test
    fun `historyRecorder fileDirectory accepts custom file directories`() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(
                HistoryRecorderOptions.Builder()
                    .fileDirectory("historyRecorder/test")
                    .build()
            )
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions)

        val fileDirectory = historyRecorder.fileDirectory()!!

        assertTrue(fileDirectory.endsWith("historyRecorder/test"))
        assertTrue(File(fileDirectory).delete())
    }

    @Test
    fun `historyRecorder stopRecording logs a warning when it is not initialized`() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions)

        historyRecorder.stopRecording {
            // do nothing
        }

        verify { logger.logW(any(), any()) }
    }

    @After
    fun tearDown() {
        unmockkStatic(LocationEngineProvider::class)
    }
}
