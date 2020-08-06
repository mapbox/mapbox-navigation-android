package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.OnboardRouterOptions
import com.mapbox.navigation.utils.internal.ThreadController
import java.io.File
import kotlinx.coroutines.withContext

internal class OnboardRouterFiles(
    val applicationContext: Context,
    val logger: Logger
) {

    suspend fun absolutePath(options: OnboardRouterOptions): String? = withContext(ThreadController.IODispatcher) {
        val fileDirectory = options.filePath ?: defaultFilePath(options)
        val tileDir = File(fileDirectory)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }
        if (tileDir.exists()) {
            logger.i(loggerTag, Message("Initial size is ${tileDir.length()} bytes"))
            tileDir.absolutePath
        } else {
            logger.e(loggerTag, Message("Unable to create a file, check the OnboardRouterOptions ${tileDir.absolutePath}"))
            null
        }
    }

    private fun defaultFilePath(options: OnboardRouterOptions): String {
        val tilesUri = options.tilesUri
        val tilesVersion = options.tilesVersion
        val directoryVersion = "Offline/${tilesUri.host}/$tilesVersion/tiles"
        return File(applicationContext.filesDir, directoryVersion).absolutePath
    }

    private companion object {
        private val loggerTag = Tag("OnboardRouterFiles")
    }
}
