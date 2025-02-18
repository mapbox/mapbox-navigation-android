package com.mapbox.navigation.mapgpt.core.textplayer

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

/**
 * A [MediaDataSource] that reads from a [VoiceAnnouncement.Remote] while it's being downloaded.
 *
 * The details of this class are specific to the behavior of the [MediaPlayer]. For example the
 * [MediaDataSource.readAt] function needs to block until the requested bytes are available.
 *
 * @param speechFileManager gives status of the download progress
 * @param remoteAnnouncement the announcement details to be played
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class MediaPlayerDataSource(
    speechFileManager: SpeechFileManager,
    private val remoteAnnouncement: VoiceAnnouncement.Remote,
) : MediaDataSource() {

    private val stateScope = MainScope()
    private val progress: StateFlow<SpeechFileProgress?> by lazy {
        speechFileManager.observeProgress(remoteAnnouncement.mediaCacheId)
            .stateIn(
                stateScope,
                SharingStarted.Eagerly,
                speechFileManager.getProgress(remoteAnnouncement.mediaCacheId),
            )
    }

    private val fileInputStream: InputStream = File(remoteAnnouncement.filePath).inputStream()
    private val bufferedStream = BufferedInputStream(fileInputStream).apply {
        mark(Int.MAX_VALUE)
    }

    override fun close() {
        SharedLog.d(TAG) { "close $remoteAnnouncement" }
        stateScope.cancel()
        bufferedStream.close()
        fileInputStream.close()
    }

    override fun readAt(position: Long, buffer: ByteArray?, offset: Int, size: Int): Int {
        bufferedStream.reset()
        bufferedStream.skip(position)
        var readCount = 0
        do {
            val innerOffset = offset + readCount
            val innerSize = size - readCount
            val innerReadCount = bufferedStream.read(buffer, innerOffset, innerSize)
            if (innerReadCount != BUFFER_END_OF_STREAM) {
                readCount += innerReadCount
            }
        } while (readCount < size && progress.value?.isDone == false)

        return readCount
    }

    override fun getSize(): Long {
        val progress = progress.value
        return if (progress?.isDone == true) {
            progress.bytesRead
        } else {
            DATA_SOURCE_UNKNOWN_SIZE
        }.also { size ->
            SharedLog.d(TAG) { "getSize $size, progress: $progress, remoteAnnouncement: $remoteAnnouncement" }
        }
    }

    private companion object {
        private const val TAG = "MediaPlayerDataSource"

        private const val BUFFER_END_OF_STREAM = -1
        private const val DATA_SOURCE_UNKNOWN_SIZE = -1L
    }
}
