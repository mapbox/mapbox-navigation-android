package com.mapbox.navigation.core.history

import android.content.Context
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import java.io.File

/**
 * Create a default directory for the history files to be saved.
 * When a directory has been provided through options, ensure it exists.
 */
internal class HistoryFiles(
    val applicationContext: Context,
    val logger: Logger
) {

    fun absolutePath(options: HistoryRecorderOptions): String? {
        val fileDirectory = options.fileDirectory ?: defaultFilePath()
        val historyFile = File(fileDirectory)
        if (!historyFile.exists()) {
            historyFile.mkdirs()
        }
        return if (historyFile.exists()) {
            logger.i(loggerTag, Message("Initial size is ${historyFile.length()} bytes"))
            historyFile.absolutePath
        } else {
            logger.e(
                loggerTag,
                Message(
                    "Unable to create a file, check the HistoryRecorderOptions " +
                        historyFile.absolutePath
                )
            )
            null
        }
    }

    private fun defaultFilePath(): String {
        return File(applicationContext.filesDir, TILES_PATH_SUB_DIR).absolutePath
    }

    private companion object {
        private val loggerTag = Tag("MbxHistoryRecorder")
        private const val TILES_PATH_SUB_DIR = "mbx_nav/history"
    }
}
