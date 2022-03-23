package com.mapbox.navigation.core.history

import android.content.Context
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import java.io.File

/**
 * Create a default directory for the history files to be saved.
 * When a directory has been provided through options, ensure it exists.
 */
internal class HistoryFiles(
    val applicationContext: Context,
) {

    fun absolutePath(options: HistoryRecorderOptions): String? {
        val fileDirectory = options.fileDirectory ?: defaultFilePath()
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
                    "check the HistoryRecorderOptions ${historyFile.absolutePath}",
                LOG_CATEGORY
            )
            null
        }
    }

    private fun defaultFilePath(): String {
        return File(applicationContext.filesDir, TILES_PATH_SUB_DIR).absolutePath
    }

    private companion object {
        private const val LOG_CATEGORY = "HistoryFiles"
        private const val TILES_PATH_SUB_DIR = "mbx_nav/history"
    }
}
