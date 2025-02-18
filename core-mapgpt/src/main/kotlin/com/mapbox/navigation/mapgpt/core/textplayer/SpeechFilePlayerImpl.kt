package com.mapbox.navigation.mapgpt.core.textplayer

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.media.VolumeShaper
import android.os.Build
import androidx.annotation.RequiresApi
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.common.w
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal class SpeechFilePlayerImpl(
    private val speechFileManager: SpeechFileManager,
    private val attributes: PlayerAttributes,
) : SpeechFilePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var clientCallback: PlayerCallback? = null
    private val playing = MutableStateFlow<VoiceAnnouncement.Remote?>(null)
    private var volume: Float = DEFAULT_VOLUME
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun play(remoteAnnouncement: VoiceAnnouncement.Remote, callback: PlayerCallback) {
        launchPlay(fadePlay = false, remoteAnnouncement, callback)
    }

    override fun fadePlay(remoteAnnouncement: VoiceAnnouncement.Remote, callback: PlayerCallback) {
        launchPlay(fadePlay = true, remoteAnnouncement, callback)
    }

    private fun launchPlay(
        fadePlay: Boolean,
        remoteAnnouncement: VoiceAnnouncement.Remote,
        callback: PlayerCallback,
    ) {
        SharedLog.d(TAG) { "launchPlay fadePlay: $fadePlay, remoteAnnouncement: $remoteAnnouncement" }
        scope.launch {
            val progress = speechFileManager.isDataAvailable(remoteAnnouncement.mediaCacheId)
            if (progress == null) {
                SharedLog.d(TAG) { "playInternal onError: Cannot play file for ${remoteAnnouncement.mediaCacheId}" }
                callback.onError(remoteAnnouncement.utteranceId, "Cannot play file")
            } else {
                try {
                    SharedLog.d(TAG) { "playInternal: $remoteAnnouncement" }
                    playInternal(remoteAnnouncement, callback, fadePlay)
                } catch (e: IllegalStateException) {
                    // MediaPlayer can throw IllegalStateException when stop() is called before
                    // the MediaPlayer is prepared. In this case, stop() would have called
                    // the callback.onStop already. If IllegalStateException is thrown without
                    // stop() being called, then it is an unexpected error so we call onError.
                    SharedLog.w(TAG, e) {
                        "playInternal failed with current: ${playing.value} " +
                            "expected: $remoteAnnouncement"
                    }
                    if (callback == this@SpeechFilePlayerImpl.clientCallback) {
                        this@SpeechFilePlayerImpl.clientCallback = null
                        SharedLog.d(TAG) { "playInternal onError: ${e.message}" }
                        callback.onError(remoteAnnouncement.utteranceId, e.message)
                    }
                } finally {
                    SharedLog.d(TAG) { "playInternal completed: $remoteAnnouncement" }
                    playing.value = null
                }
            }
        }
    }

    private suspend fun playInternal(
        remoteAnnouncement: VoiceAnnouncement.Remote,
        callback: PlayerCallback,
        fadePlay: Boolean,
    ) {
        if (mediaPlayer != null) {
            SharedLog.d(TAG) { "playInternal mediaPlayer is not null, stop previous player" }
            stop()
        }
        this.clientCallback = callback
        playing.value = remoteAnnouncement
        val mediaPlayer = MediaPlayer()
        this.mediaPlayer = mediaPlayer
        attributes.applyOn(mediaPlayer)

        // Long running operation
        mediaPlayer.setVoiceAnnouncementDataSource(remoteAnnouncement)

        mediaPlayer.setVolume(volume, volume)
        mediaPlayer.setOnCompletionListener { mp ->
            SharedLog.d(TAG) { "onComplete: $remoteAnnouncement" }
            val previousCallback = clientCallback
            clientCallback = null
            this@SpeechFilePlayerImpl.mediaPlayer = null
            mp.reset()
            mp.release()
            previousCallback?.onComplete(
                remoteAnnouncement.text,
                remoteAnnouncement.utteranceId
            )
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            val previousCallback = clientCallback
            clientCallback = null
            val reason = "MediaPlayer: $mp, what: $what, extra: $extra"
            this@SpeechFilePlayerImpl.mediaPlayer = null
            mp.reset()
            mp.release()
            SharedLog.d(TAG) { "onError: $remoteAnnouncement, $reason" }
            previousCallback?.onError(remoteAnnouncement.utteranceId, reason)
            true
        }

        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener { mp ->
            SharedLog.d(TAG) { "onPrepared: $remoteAnnouncement" }
            remoteAnnouncement.progress?.let { progress ->
                mp.seekTo(progress.milliseconds)
            }
            if (fadePlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SharedLog.d(TAG) { "fadePlay with VolumeShaper" }
                mp.safePlayVolumeShaper(
                    VolumeShaper.Configuration.Builder()
                        .setDuration(FADE_DURATION.inWholeMilliseconds)
                        .setCurve(floatArrayOf(0f, 1f), floatArrayOf(0f, volume))
                        .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
                        .build()
                )
            }
            mp.start()
            callback.onStartPlaying(remoteAnnouncement.text, remoteAnnouncement.utteranceId)
        }
    }

    override suspend fun fadeStop() {
        val playingAnnouncement = playing.value
        val progress = playingAnnouncement?.let { speechFileManager.getProgress(it.mediaCacheId) }
        SharedLog.d(TAG) {
            "fadeStop duration: ${mediaPlayer?.duration}," +
                " position: ${mediaPlayer?.currentPosition}, " +
                " playingAnnouncement: $playingAnnouncement, " +
                " progress: $progress"
        }
        val mediaPlayer = mediaPlayer ?: run {
            SharedLog.w(TAG) { "fadeStop mediaPlayer is null" }
            stopInternal(playingAnnouncement?.utteranceId, VoiceProgress.None)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SharedLog.d(TAG) { "fadeStop apply VolumeShaper" }
            val voiceProgress = currentVoiceProgress()
            val applied = mediaPlayer.safePlayVolumeShaper(fadeStopConfig)
            if (applied) delay(FADE_DURATION)
            stopInternal(playingAnnouncement?.utteranceId, voiceProgress)
        } else {
            stop()
        }
    }

    override fun stop() {
        SharedLog.d(TAG) { "stop" }
        val utteranceId = playing.value?.utteranceId.orEmpty()
        stopInternal(utteranceId, currentVoiceProgress())
    }

    override fun volume(level: Float) {
        SharedLog.d(TAG) { "volume $level" }
        if (level in 0.0f..1.0f) {
            volume = level
            mediaPlayer?.setVolume(volume, volume)
        }
    }

    override fun release() {
        SharedLog.d(TAG) { "release" }
        scope.coroutineContext.cancelChildren()
        scope.launch {
            val releaseMediaPlayer = mediaPlayer
            mediaPlayer = null
            releaseMediaPlayer?.reset()
            releaseMediaPlayer?.release()
            clientCallback = null
        }
    }

    /**
     * When using API 23 and above, play the speech file before it has finished downloading.
     * When using API 22 and below, play the speech file after it has finished downloading.
     *
     * The [MediaDataSource] has better streaming control, this means the SDK requires API 23 to
     * support simultaneous downloading and playing of the speech file.
     */
    private suspend fun MediaPlayer.setVoiceAnnouncementDataSource(
        announcement: VoiceAnnouncement.Remote,
    ) = withContext(Dispatchers.IO) {
        if (STREAMING_ENABLED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setDataSource(MediaPlayerDataSource(speechFileManager, announcement))
        } else {
            speechFileManager.observeProgress(announcement.mediaCacheId)
                .filterNotNull()
                .first { it.isDone }
            setDataSource(announcement.filePath)
        }
    }

    private fun currentVoiceProgress(): VoiceProgress {
        val mediaPlayer = mediaPlayer ?: return VoiceProgress.None
        return try {
            VoiceProgress.Time(mediaPlayer.currentPosition)
        } catch (e: IllegalStateException) {
            VoiceProgress.None
        }
    }

    private fun stopInternal(utteranceId: String?, voiceProgress: VoiceProgress) {
        SharedLog.d(TAG) { "stopInternal utteranceId: $utteranceId, voiceProgress: $voiceProgress" }
        mediaPlayer?.stop()

        val callback = clientCallback
        clientCallback = null
        playing.value = null

        callback?.onStop(utteranceId.orEmpty(), voiceProgress)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun MediaPlayer.safePlayVolumeShaper(
        configuration: VolumeShaper.Configuration,
    ): Boolean {
        return try {
            val shaper = createVolumeShaper(configuration)
            shaper.apply(VolumeShaper.Operation.PLAY)
            true
        } catch (ex: Exception) {
            // Kotlin syntax for catching multiple exceptions
            // https://stackoverflow.com/questions/36760489/how-to-catch-many-exceptions-at-the-same-time-in-kotlin
            when(ex) {
                is IllegalArgumentException, is IllegalStateException -> {
                    // Expected exceptions when the MediaPlayer is not in the correct state.
                    SharedLog.w(TAG, ex) { "VolumeShaper failed" }
                }
                else -> throw ex
            }
            false
        }
    }

    private companion object {
        private const val TAG = "SpeechFilePlayer"

        private const val DEFAULT_VOLUME = 1.0f

        // Set to false to disable the streaming feature. The streaming feature will allow the
        // MediaPlayer to start playing the speech file before it has finished downloading.
        private const val STREAMING_ENABLED = true

        // The soft stop fade duration in milliseconds.
        private val FADE_DURATION = 1500.milliseconds

        @RequiresApi(Build.VERSION_CODES.O)
        private val fadeStopConfig = VolumeShaper.Configuration.Builder()
            .setDuration(FADE_DURATION.inWholeMilliseconds)
            .setCurve(floatArrayOf(0f, 1f), floatArrayOf(1f, 0f))
            .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
            .build()
    }
}
