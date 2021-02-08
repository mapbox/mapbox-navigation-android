package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.OnboardRouterOptions
import java.io.File

internal class OnboardRouterFiles(
    val applicationContext: Context,
    val logger: Logger
) {

    fun absolutePath(options: OnboardRouterOptions): String {
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
                    "Unable to create a file, check the OnboardRouterOptions " +
                        tileDir.absolutePath
                )
            )
            ""
        }
    }

    private fun defaultFilePath(): String =
        File(applicationContext.filesDir, DEFAULT_TILES_FOLDER_NAME).absolutePath

    private companion object {
        private const val DEFAULT_TILES_FOLDER_NAME = "Offline"
        private val loggerTag = Tag("OnboardRouterFiles")
    }
}
