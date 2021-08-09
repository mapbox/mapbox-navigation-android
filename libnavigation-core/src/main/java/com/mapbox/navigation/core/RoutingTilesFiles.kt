package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.RoutingTilesOptions
import java.io.File

internal class RoutingTilesFiles(
    val applicationContext: Context,
    val logger: Logger
) {

    fun absolutePath(options: RoutingTilesOptions): String {
        val fileDirectory = options.filePath ?: defaultFilePath()
        val tileDir = File(fileDirectory)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }
        return if (tileDir.exists()) {
            logger.i(loggerTag, Message("Initial size is ${tileDir.length()} bytes"))
            tileDir.absolutePath
        } else {
            logger.e(
                loggerTag,
                Message(
                    "Unable to create a file, check the RoutingTilesOptions " +
                        tileDir.absolutePath
                )
            )
            ""
        }
    }

    private fun defaultFilePath(): String {
        return File(applicationContext.filesDir, TILES_PATH_SUB_DIR).absolutePath
    }

    internal companion object {
        private val loggerTag = Tag("MbxRoutingTilesOptions")
        internal const val TILES_PATH_SUB_DIR = "mbx_nav/tiles"
    }
}
