package com.mapbox.navigation.core.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.internal.history.HistoryRecordingEnabledObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigator.HistoryRecorderHandleInterface
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
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
    private val observer = mockk<HistoryRecordingEnabledObserver>(relaxed = true)
    private val handle = mockk<HistoryRecorderHandleInterface>(relaxed = true)

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
                    .build(),
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
                    .build(),
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

    @Test
    fun registerObserverStickyNoRecorder() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions)

        historyRecorder.registerHistoryRecordingEnabledObserver(observer)

        verify(exactly = 1) {
            observer.onDisabled(historyRecorder)
        }
    }

    @Test
    fun registerObserverStickyTrue() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, handle)
        historyRecorder.startRecording()

        historyRecorder.registerHistoryRecordingEnabledObserver(observer)

        verify(exactly = 1) {
            observer.onEnabled(historyRecorder)
        }
    }

    @Test
    fun registerObserverStickyFalse() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, handle)

        historyRecorder.registerHistoryRecordingEnabledObserver(observer)

        verify(exactly = 1) {
            observer.onDisabled(historyRecorder)
        }
    }

    @Test
    fun observerGetsNotifiedAboutChanges() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, handle)

        historyRecorder.registerHistoryRecordingEnabledObserver(observer)
        clearAllMocks(answers = false)

        historyRecorder.startRecording()

        verify(exactly = 1) {
            observer.onEnabled(historyRecorder)
        }

        historyRecorder.stopRecording({})

        verify(exactly = 1) {
            observer.onDisabled(historyRecorder)
        }
    }

    @Test
    fun observerDoesNotGetNotifiedAfterUnregister() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, handle)

        historyRecorder.registerHistoryRecordingEnabledObserver(observer)
        clearAllMocks(answers = false)

        historyRecorder.unregisterHistoryRecordingEnabledObserver(observer)

        historyRecorder.startRecording()

        verify(exactly = 0) {
            observer.onEnabled(any())
            observer.onDisabled(any())
        }
    }

    @Test
    fun unregisterAllObservers() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, handle)

        historyRecorder.registerHistoryRecordingEnabledObserver(observer)
        clearAllMocks(answers = false)

        historyRecorder.unregisterAllHistoryRecordingEnabledObservers()

        historyRecorder.startRecording()

        verify(exactly = 0) {
            observer.onEnabled(any())
            observer.onDisabled(any())
        }
    }

    @Test
    fun unregisterObserverInObserverDoesNotCrash() {
        val navigationOptions = navigationOptionsBuilder
            .historyRecorderOptions(HistoryRecorderOptions.Builder().build())
            .build()
        val historyRecorder = MapboxHistoryRecorder(navigationOptions, handle)

        val observer = object : HistoryRecordingEnabledObserver {
            override fun onEnabled(historyRecorderHandle: MapboxHistoryRecorder) {
                historyRecorder.unregisterHistoryRecordingEnabledObserver(this)
            }

            override fun onDisabled(historyRecorderHandle: MapboxHistoryRecorder) {
            }
        }
        historyRecorder.registerHistoryRecordingEnabledObserver(observer)

        historyRecorder.startRecording()
    }
}
