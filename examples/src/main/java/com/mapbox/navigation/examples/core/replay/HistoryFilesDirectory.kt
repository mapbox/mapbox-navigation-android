package com.mapbox.navigation.examples.core.replay

import android.content.Context
import java.io.File

/**
 * Helper class to keep the a consistent directory.
 */
class HistoryFilesDirectory {
    /**
     * The directory where the replay files are stored.
     */
    fun replayDirectory(context: Context) =
        File(context.filesDir, DIRECTORY_NAME).also { it.mkdirs() }

    /**
     * Returns a list of history files from the [replayDirectory].
     */
    fun replayFiles(context: Context) =
        replayDirectory(context).walk().filterNot { it.isDirectory }.toList()

    /**
     * Returns a file in the [replayDirectory] where a history file can be written.
     */
    fun outputFile(context: Context, path: String) =
        File(replayDirectory(context), path)

    private companion object {
        const val DIRECTORY_NAME = "replay"
    }
}
