package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.reachability.SharedReachability
import com.mapbox.navigation.mapgpt.core.config.api.RemoteTtsProvider
import com.mapbox.navigation.mapgpt.core.utils.obfuscateStackTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class RemoteTTSPlayerImpl(
    private val remoteTTSApiClient: RemoteTTSApiClient,
    private val speechFileManager: SpeechFileManager,
    private val speechFilePlayer: SpeechFilePlayer,
    private val sharedReachability: SharedReachability,
) : RemoteTTSPlayer {
    @RemoteTtsProvider
    override val provider: String = remoteTTSApiClient.provider

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val availableLanguages: StateFlow<Set<Language>> = MutableStateFlow(
        remoteTTSApiClient.availableLanguages(),
    )
    override val availableVoices: StateFlow<Set<Voice>> = MutableStateFlow(
        remoteTTSApiClient.availableVoices(),
    )
    private val outstandingRequests = MutableStateFlow<Set<String>>(emptySet())

    override fun prefetch(announcement: Announcement) {
        val measure = announcement.measureDownload()
        if (!sharedReachability.isReachable.value) {
            measure.log { "no internet connection so download did not start" }
            return
        }
        val mediaCacheId = announcement.mediaCacheId
        outstandingRequests.value += mediaCacheId
        ioScope.launch {
            speechFileManager.requestSpeechFile(remoteTTSApiClient, announcement).fold(
                onSuccess = {
                    measure.log { "speech file success: $it" }
                    outstandingRequests.value -= mediaCacheId
                },
                onFailure = {
                    measure.log { "error: ${it.obfuscateStackTrace()}" }
                    outstandingRequests.value -= mediaCacheId
                },
            )
            measure.log { "prefetch completed ${speechFileManager.getProgress(mediaCacheId)}" }
        }
    }

    override suspend fun prepare(announcement: Announcement): Result<String> {
        val mediaCacheId = announcement.mediaCacheId
        speechFileManager.getProgress(mediaCacheId)?.let {
            return Result.success(it.filePath)
        }
        if (!outstandingRequests.value.contains(mediaCacheId)) {
            return Result.failure(RuntimeException("Speech file is not downloading"))
        }
        val progress: SpeechFileProgress? = waitForProgress(mediaCacheId)
        return if (progress == null) {
            Result.failure(RuntimeException("Speech file is unavailable"))
        } else {
            Result.success(progress.filePath)
        }
    }

    override fun play(voice: VoiceAnnouncement.Remote, callback: PlayerCallback) {
        speechFilePlayer.play(voice, callback)
    }

    override fun fadePlay(voice: VoiceAnnouncement.Remote, callback: PlayerCallback) {
        speechFilePlayer.fadePlay(voice, callback)
    }

    override fun stop() {
        speechFilePlayer.stop()
    }

    override suspend fun fadeStop() {
        speechFilePlayer.fadeStop()
    }

    override fun volume(level: Float) {
        speechFilePlayer.volume(level)
    }

    override fun release() {
        speechFilePlayer.release()
        speechFileManager.clear()
        ioScope.coroutineContext.cancelChildren()
        outstandingRequests.value = emptySet()
    }

    /**
     * If there is an outstanding request for the speech file, this function waits for the file to
     * be created by the [SpeechFileManager] and returns the progress. If no progress is made
     * within the timeout, the function returns null. If the outstanding request is removed before
     * the file is created, the function returns null.
     *
     * TODO Implement more flexible timeout mechanism
     * https://mapbox.atlassian.net/browse/NAVGPT-662
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun waitForProgress(mediaCacheId: String): SpeechFileProgress? {
        return withTimeoutOrNull(WAIT_FOR_PROGRESS_TIMEOUT) {
            outstandingRequests.flatMapLatest { mediaCacheIds ->
                if (mediaCacheIds.contains(mediaCacheId)) {
                    speechFileManager.observeProgress(mediaCacheId)
                        .dropWhile { progress ->
                            val bytesRead = progress?.bytesRead ?: 0
                            bytesRead < WAIT_FOR_PROGRESS_BYTES
                        }
                } else {
                    flowOf(null)
                }
            }.first()
        }
    }

    companion object {

        /**
         * The maximum time to wait for the first bytes of the audio file to be available.
         */
        private const val WAIT_FOR_PROGRESS_TIMEOUT = 10_000L

        /**
         * The minimum number of bytes to consider the audio file available for playback.
         */
        private const val WAIT_FOR_PROGRESS_BYTES = 4L
    }
}
