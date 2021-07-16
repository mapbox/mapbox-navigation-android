package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.NavigationFiles.DirectoryType.CACHE
import com.mapbox.navigation.core.NavigationFiles.DirectoryType.HISTORY
import java.io.File

internal class NavigationFiles(
    val applicationContext: Context,
    val logger: Logger,
) {

    fun historyAbsolutePath(options: NavigationOptions): String =
        absolutePath(options.historyRecorderOptions.fileDirectory, HISTORY)

    fun cacheAbsolutePath(options: NavigationOptions): String =
        absolutePath(options.routingTilesOptions.filePath, CACHE)

    private fun absolutePath(filePath: String?, type: DirectoryType): String {
        val fileDirectory = filePath ?: defaultFilePath(type.defaultPath)
        val navDir = File(fileDirectory)
        if (!navDir.exists()) {
            navDir.mkdirs()
        }
        return if (navDir.exists()) {
            logger.i(loggerTag, Message("Initial size of $type dir is ${navDir.length()} bytes"))
            navDir.absolutePath
        } else {
            logger.e(
                loggerTag,
                Message("Unable to create $type dir, check the options ${navDir.absolutePath}")
            )
            EMPTY_PATH
        }
    }

    private fun defaultFilePath(path: String): String {
        return File(applicationContext.filesDir, path).absolutePath
    }

    private enum class DirectoryType(val defaultPath: String) {
        CACHE(CACHE_DEFAULT_PATH),
        HISTORY(HISTORY_DEFAULT_PATH)
    }

    internal companion object {
        private val loggerTag = Tag("MbxNavigationFiles")
        private const val EMPTY_PATH = ""
        internal const val CACHE_DEFAULT_PATH = "mbx_nav/cache"
        internal const val HISTORY_DEFAULT_PATH = "mbx_nav/history"
    }
}
