package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import java.io.File

internal class RoutingTilesFiles(
    val applicationContext: Context,
) {

    fun absolutePath(options: RoutingTilesOptions): String {
        val fileDirectory = options.filePath ?: defaultFilePath()
        val tileDir = File(fileDirectory)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }
        return if (tileDir.exists()) {
            logI("Initial size is ${tileDir.length()} bytes", LOG_CATEGORY)
            tileDir.absolutePath
        } else {
            logE(
                "Unable to create a file, check the RoutingTilesOptions ${tileDir.absolutePath}",
                LOG_CATEGORY,
            )
            ""
        }
    }

    private fun defaultFilePath(): String {
        return File(applicationContext.filesDir, TILES_PATH_SUB_DIR).absolutePath
    }

    internal companion object {
        private const val LOG_CATEGORY = "RoutingTilesFiles"
        internal const val TILES_PATH_SUB_DIR = "mbx_nav/tiles"
    }
}
