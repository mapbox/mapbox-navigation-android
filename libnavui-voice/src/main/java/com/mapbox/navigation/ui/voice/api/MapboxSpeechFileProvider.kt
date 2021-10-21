package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File

internal class MapboxSpeechFileProvider(private val cacheDirectory: File) {

    private val ioJobController by lazy { InternalJobControlFactory.createIOScopeJobControl() }

    suspend fun generateVoiceFileFrom(data: ResponseBody): File =
        withContext(ThreadController.IODispatcher) {
            // OS can delete folders and files in the cache even while app is running.
            cacheDirectory.mkdirs()
            File(cacheDirectory, "${retrieveUniqueId()}$MP3_EXTENSION")
                .apply { outputStream().use { data.byteStream().copyTo(it) } }
        }

    fun delete(file: File) {
        ioJobController.scope.launch {
            file.delete()
        }
    }

    fun cancel() {
        ioJobController.job.cancelChildren()
    }

    private fun retrieveUniqueId(): String = (++uniqueId).toString()

    private companion object {
        private const val MP3_EXTENSION = ".mp3"
        private var uniqueId = 0
    }
}
