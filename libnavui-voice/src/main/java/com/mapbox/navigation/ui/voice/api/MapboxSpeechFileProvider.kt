package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File

internal class MapboxSpeechFileProvider(context: Context) {

    private val ioJobController: JobControl by lazy { ThreadController.getIOScopeAndRootJob() }
    private val instructionsCacheDirectory: File =
        File(context.applicationContext.cacheDir, MAPBOX_INSTRUCTIONS_CACHE).also { it.mkdirs() }

    suspend fun generateVoiceFileFrom(data: ResponseBody): File =
        withContext(ThreadController.IODispatcher) {
            File(instructionsCacheDirectory, "${retrieveUniqueId()}$MP3_EXTENSION")
                .apply { outputStream().use { data.byteStream().copyTo(it) } }
        }

    private fun retrieveUniqueId(): String = (++uniqueId).toString()

    fun delete(file: File) {
        ioJobController.scope.launch {
            file.delete()
        }
    }

    private companion object {
        private const val MAPBOX_INSTRUCTIONS_CACHE = "mapbox_instructions_cache"
        private const val MP3_EXTENSION = ".mp3"
        private var uniqueId = 0
    }
}
