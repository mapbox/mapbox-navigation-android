package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.audio.AudioInfoRetriever
import com.mapbox.navigation.mapgpt.core.performance.DashMeasure
import com.mapbox.navigation.mapgpt.core.performance.SharedPerformance
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class SpeechFileManagerImpl(
    cacheDirectory: File,
) : SpeechFileManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val progressMap = MutableStateFlow<Map<String, SpeechFileProgress>>(emptyMap())
    private val speechFiles = File(cacheDirectory, SPEECH_FILES_DIRECTORY)
    private val requestDownloadMutex = Mutex()

    init {
        scope.launch {
            updateProgressMapFromCache()
            deleteIncompleteFiles()
        }
    }

    override fun observeProgress(mediaCacheId: String): Flow<SpeechFileProgress?> {
        SharedLog.d(TAG) { "observeProgress $mediaCacheId, size=${progressMap.value.size}, keys=${progressMap.value.keys.joinToString()}" }
        return progressMap.map { it[mediaCacheId] }
    }

    override fun getProgress(mediaCacheId: String): SpeechFileProgress? {
        return progressMap.value[mediaCacheId]
    }

    override suspend fun requestSpeechFile(
        remoteTTSApiClient: RemoteTTSApiClient,
        announcement: Announcement
    ): Result<String> {
        val existingProgress = requestDownloadMutex.withLock {
            val mediaCacheId = announcement.mediaCacheId
            getProgress(mediaCacheId) ?: run {
                progressUpdate(announcement, false)
                null
            }
        }
        if (existingProgress != null) {
            return Result.success(existingProgress.filePath)
        }

        val measure = announcement.measureDownload()
        measure.start { announcement.text }
        SharedLog.d(TAG) { "requestSpeechFile ${announcement.text} with ${remoteTTSApiClient.provider}" }
        return remoteTTSApiClient.requestAudioBytes(announcement.text)
            .mapCatching { inputChannel ->
                val progress = transferBytesToMediaFile(
                    inputChannel = inputChannel,
                    announcement = announcement,
                )
                SharedLog.d(TAG) { "transfer started: $progress" }
                progress?.filePath ?: throw RuntimeException("Unable to download audio file")
            }.onFailure { e ->
                SharedLog.w(TAG) { "Failed to request audio bytes: $e" }
            }
    }

    override fun delete(mediaCacheId: String) {
        scope.launch {
            if (progressMap.value[mediaCacheId]?.isDone == false) {
                SharedLog.w(TAG) { "Cannot delete in-progress file $mediaCacheId" }
            } else {
                deleteMediaAndProgressFiles(mediaCacheId)
            }
        }
    }

    override fun clear() {
        SharedLog.w(TAG) { "clear" }
        scope.launch {
            evictOldestFilesIfOverLimit()
        }
    }

    private fun deleteMediaAndProgressFiles(mediaCacheId: String) {
        SharedLog.d(TAG) { "delete $mediaCacheId" }
        getMediaFile(mediaCacheId).delete()
        getProgressFile(mediaCacheId).delete()
        progressMap.update { currentMap ->
            currentMap - mediaCacheId
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun transferBytesToMediaFile(
        inputChannel: ByteReadChannel,
        announcement: Announcement,
    ): SpeechFileProgress? {
        val measure = announcement.measureDownload()
        val outputFile = getMediaFile(announcement.mediaCacheId).apply { delete() }

        withContext(Dispatchers.IO) {
            BufferedOutputStream(outputFile.outputStream()).use { outputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (inputChannel.availableForRead == 0) {
                    delay(CHECK_AVAILABLE_DELAY)
                }
                measure.mark(MARK_BYTES_AVAILABLE) { "${inputChannel.availableForRead}" }
                var startTime = TimeSource.Monotonic.markNow()
                while (true) {
                    val readBytes = throttledBytesToRead(startTime)
                    val bytesRead = inputChannel.readAvailable(buffer, 0, readBytes)
                    startTime = TimeSource.Monotonic.markNow()
                    if (bytesRead == READ_BUFFER_CLOSED) break

                    outputStream.write(buffer, 0, bytesRead)
                    progressUpdate(announcement, false)
                }
            }
        }

        return progressUpdate(announcement, true)?.also { finalProgress ->
            markDownloadCompleted(measure, finalProgress)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun throttledBytesToRead(lastReadTime: TimeSource.Monotonic.ValueTimeMark): Int {
        val currentTime = TimeSource.Monotonic.markNow()
        val timeElapsed = (currentTime - lastReadTime).toDouble(DurationUnit.SECONDS)
        val maxBitsToRead = MAX_DOWNLOAD_BITRATE * timeElapsed
        return min(DEFAULT_BUFFER_SIZE, (maxBitsToRead / 8).toInt())
    }

    private fun updateProgressMapFromCache() {
        progressMap.value = try {
            getProgressMapFromCache()
        } catch (e: SerializationException) {
            speechFiles.deleteRecursively()
            emptyMap()
        }
    }

    @Throws(SerializationException::class)
    private fun getProgressMapFromCache(): Map<String, SpeechFileProgress> {
        if (!speechFiles.exists()) speechFiles.mkdirs()
        val progress = speechFiles.listFiles()
            ?.filter { it.name.endsWith(JSON_EXTENSION) }
            ?.mapNotNull { file ->
                Json.decodeFromString(
                    SpeechFileProgress.serializer(),
                    file.readText(),
                )
            }
            ?.associateBy { it.mediaCacheId }
        return progress ?: emptyMap()
    }

    private suspend fun evictOldestFilesIfOverLimit() = withContext(scope.coroutineContext) {
        val completedDownloads = progressMap.value.filterValues { it.isDone }.values
        var bytesOverLimit = completedDownloads.sumOf { it.bytesRead } - CACHE_MAX_SIZE_BYTES
        if (bytesOverLimit > 0) {
            val filesToKeep = completedDownloads.map { File(it.filePath) }
                .sortedByDescending { it.lastModified() }
                .toMutableList()
            val filesToDelete = mutableListOf<File>()
            while (bytesOverLimit > 0) {
                val file = filesToKeep.removeLast()
                filesToDelete.add(file)
                bytesOverLimit -= file.length()
            }
            SharedLog.d(TAG) { "Evicting ${filesToDelete.size} files" }
            filesToDelete.forEach { delete(it.nameWithoutExtension) }
        }
    }

    private suspend fun deleteIncompleteFiles() = withContext(scope.coroutineContext) {
        val incompleteDownloads = progressMap.value.filterValues { !it.isDone }.values
        incompleteDownloads.forEach { deleteMediaAndProgressFiles(it.mediaCacheId) }
    }

    private suspend fun progressUpdate(
        announcement: Announcement,
        isDone: Boolean,
    ): SpeechFileProgress? = withContext(Dispatchers.IO) {
        val mediaCacheId = announcement.mediaCacheId
        val mediaFile = getMediaFile(mediaCacheId)
        val progress = SpeechFileProgress(
            mediaCacheId = mediaCacheId,
            text = announcement.text,
            filePath = mediaFile.absolutePath,
            bytesRead = mediaFile.length(),
            isDone = isDone,
        )
        progressMap.updateAndGet { previousMap ->
            previousMap + (mediaCacheId to progress)
        }[mediaCacheId]?.also {
            val progressFile = getProgressFile(mediaCacheId)
            val progressJson = Json.encodeToString(SpeechFileProgress.serializer(), progress)
            progressFile.writeText(progressJson)
        }
    }

    private fun getMediaFile(mediaCacheId: String): File {
        val mp3FileName = "$mediaCacheId$MP3_EXTENSION"
        if (!speechFiles.exists()) speechFiles.mkdirs()
        return File(speechFiles, mp3FileName)
    }

    private fun getProgressFile(mediaCacheId: String): File {
        val jsonFileName = "$mediaCacheId$JSON_EXTENSION"
        if (!speechFiles.exists()) speechFiles.mkdirs()
        return File(speechFiles, jsonFileName)
    }

    private fun markDownloadCompleted(dashMeasure: DashMeasure, finalProgress: SpeechFileProgress) {
        scope.launch(Dispatchers.IO) {
            SharedPerformance.complete(dashMeasure) {
                val durationSince = dashMeasure.elapsedSince(MARK_BYTES_AVAILABLE) ?: Duration.ZERO
                val elapsedTime = durationSince.toDouble(DurationUnit.SECONDS)
                val bitsRead = finalProgress.bytesRead * 8.0
                val bitrate = (bitsRead / elapsedTime).toInt()
                val outputFile = getMediaFile(finalProgress.mediaCacheId)
                val audioMetadata = AudioInfoRetriever.audioMetadataMap(outputFile).toMutableMap()
                audioMetadata["download_bitsRead"] = bitsRead.toString()
                audioMetadata["download_elapsedTime"] = elapsedTime.toString()
                audioMetadata["download_bitrate"] = bitrate.toString()
                audioMetadata.toString()
            }
        }
    }

    private companion object {
        private const val TAG = "SpeechFileManager"

        private const val MP3_EXTENSION = ".mp3"
        private const val JSON_EXTENSION = ".json"
        private const val CACHE_MAX_SIZE_BYTES: Long = 200 * 1024 * 1024
        private const val SPEECH_FILES_DIRECTORY = "/mbx_dash_sdk_speech_files/"
        private const val CHECK_AVAILABLE_DELAY = 10L
        private const val READ_BUFFER_CLOSED = -1

        private const val MARK_BYTES_AVAILABLE = "bytes_available"

        private const val MAX_DOWNLOAD_BITRATE = Long.MAX_VALUE
        // Throttle download speed for benchmarking and testing
//        private const val MAX_DOWNLOAD_BITRATE = 128_000L
    }
}
