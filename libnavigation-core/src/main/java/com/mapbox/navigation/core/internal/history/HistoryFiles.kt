package com.mapbox.navigation.core.internal.history

import android.content.Context
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import java.io.File

/**
 * Create a default directory for the history files to be saved.
 * When a directory has been provided through options, ensure it exists.
 */
class HistoryFiles(
    val applicationContext: Context,
) {

    fun historyRecorderAbsolutePath(options: HistoryRecorderOptions): String? {
        val fileDirectory = options.fileDirectory ?: defaultFilePath()
        return absolutePath(fileDirectory)
    }

    fun copilotAbsolutePath(): String? {
        val fileDirectory = copilotFilePath()
        return absolutePath(fileDirectory)
    }

    private fun absolutePath(fileDirectory: String): String? {
        val historyFile = File(fileDirectory)
        if (!historyFile.exists()) {
            historyFile.mkdirs()
        }
        return if (historyFile.exists()) {
            logI("Initial size is ${historyFile.length()} bytes", LOG_CATEGORY)
            historyFile.absolutePath
        } else {
            logE(
                "Unable to create a file, " +
                    "it may be the HistoryRecorderOptions ${historyFile.absolutePath}",
                LOG_CATEGORY,
            )
            null
        }
    }

    private fun defaultFilePath(): String {
        return File(applicationContext.filesDir, HISTORY_PATH_SUB_DIR).absolutePath
    }

    private fun copilotFilePath(): String {
        return File(applicationContext.filesDir, COPILOT_PATH_SUB_DIR).absolutePath
    }

    private companion object {

        private const val LOG_CATEGORY = "HistoryFiles"
        private const val HISTORY_PATH_SUB_DIR = "mbx_nav/history"
        private const val COPILOT_PATH_SUB_DIR = "mbx_nav/copilot/history"
    }
}
