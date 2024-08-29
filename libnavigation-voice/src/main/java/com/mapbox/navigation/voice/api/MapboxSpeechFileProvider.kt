package com.mapbox.navigation.voice.api

import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

internal class MapboxSpeechFileProvider(private val cacheDirectory: File) {

    private val ioJobController by lazy { InternalJobControlFactory.createIOScopeJobControl() }

    suspend fun generateVoiceFileFrom(inputStream: InputStream): File =
        withContext(ThreadController.IODispatcher) {
            // OS can delete folders and files in the cache even while app is running.
            cacheDirectory.mkdirs()
            File(cacheDirectory, "${retrieveUniqueId()}$MP3_EXTENSION").apply {
                outputStream().use { os -> inputStream.copyTo(os) }
            }
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
