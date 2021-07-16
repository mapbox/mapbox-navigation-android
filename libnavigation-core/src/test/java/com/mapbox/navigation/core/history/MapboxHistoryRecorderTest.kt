package com.mapbox.navigation.core.history

import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.core.history.MapboxHistoryRecorder.Companion.MESSAGE
import com.mapbox.navigation.core.history.MapboxHistoryRecorder.Companion.TAG
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxHistoryRecorderTest {

    private val logger: Logger = mockk(relaxUnitFun = true)
    private val historyRecorder = MapboxHistoryRecorder(logger, "")

    @Test
    fun `historyRecorder startRecording logs a warning when it is not initialized`() {
        historyRecorder.startRecording()

        verify { logger.w(Tag(TAG), Message(MESSAGE)) }
    }

    @Test
    fun `historyRecorder stopRecording logs a warning when it is not initialized`() {
        historyRecorder.stopRecording {
            // do nothing
        }

        verify { logger.w(Tag(TAG), Message(MESSAGE)) }
    }

    @Test
    fun `historyRecorder pushHistory logs a warning when it is not initialized`() {
        historyRecorder.pushHistory("type", "event")

        verify { logger.w(Tag(TAG), Message(MESSAGE)) }
    }
}
