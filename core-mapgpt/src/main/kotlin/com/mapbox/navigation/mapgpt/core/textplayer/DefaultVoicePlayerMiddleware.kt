package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.audiofocus.AudioFocusOwner
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.performance.DashTrace
import com.mapbox.navigation.mapgpt.core.performance.SharedPerformance
import com.mapbox.navigation.mapgpt.core.performance.TraceKey
import com.mapbox.navigation.mapgpt.core.performance.TraceName
import com.mapbox.navigation.mapgpt.core.performance.TraceValue
import com.mapbox.navigation.mapgpt.core.textplayer.middleware.VoicePlayerMiddleware
import com.mapbox.navigation.mapgpt.core.textplayer.middleware.VoicePlayerMiddlewareContext
import com.mapbox.navigation.mapgpt.core.utils.obfuscateStackTrace
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch


internal class DefaultVoicePlayerMiddleware(
    private val localTTSPlayer: LocalTTSPlayer,
    val remoteTTSPlayer: RemoteTTSPlayerSwitcher,
) : VoicePlayerMiddleware, CoroutineMiddleware<VoicePlayerMiddlewareContext>() {

    init {
        SpeechPlayerPerformance.enableAll()
    }

    private var playingJob: Job? = null

    private val _availableLanguages = MutableStateFlow(emptySet<Language>())
    override val availableLanguages: StateFlow<Set<Language>> = _availableLanguages.asStateFlow()
    private val _availableVoices = MutableStateFlow(emptySet<Voice>())
    override val availableVoices: StateFlow<Set<Voice>> = _availableVoices.asStateFlow()

    override fun onAttached(middlewareContext: VoicePlayerMiddlewareContext) {
        super.onAttached(middlewareContext)
        SharedLog.i(TAG) { "onAttached" }
        middlewareContext.bindVoiceCapabilities()
        remoteTTSPlayer.onAttached(middlewareContext)
    }

    override fun onDetached(middlewareContext: VoicePlayerMiddlewareContext) {
        super.onDetached(middlewareContext)
        SharedLog.i(TAG) { "onDetached" }
        remoteTTSPlayer.onDetached(middlewareContext)
        _availableLanguages.value = emptySet()
        _availableVoices.value = emptySet()
        release()
    }

    override fun prefetch(announcement: Announcement) {
        val preferLocalTts = middlewareContext?.preferLocalTts?.value ?: run {
            SharedLog.e(TAG) { "cannot prefetch when detached" }
            return
        }
        if (!preferLocalTts) {
            remoteTTSPlayer.prefetch(announcement)
        }
    }

    override fun play(
        announcement: Announcement,
        progress: VoiceProgress?,
        callback: PlayerCallback,
    ) {
        SharedLog.d(TAG) { "play $announcement" }
        val middlewareContext = middlewareContext ?: run {
            callback.onError(announcement.utteranceId, "middleware not attached")
            return
        }

        val trace = SharedPerformance.newTrace(TraceName.TEXT_TO_SPEECH_STARTED).start()
        playingJob = ioScope.launch {
            middlewareContext.playInternal(
                announcement = announcement,
                progress = progress ?: VoiceProgress.None,
                fadePlay = false,
                trace = trace,
                callback = callback,
            )
        }
    }

    override fun fadePlay(
        announcement: Announcement,
        progress: VoiceProgress?,
        callback: PlayerCallback
    ) {
        SharedLog.d(TAG) { "fadePlay $announcement" }
        val middlewareContext = middlewareContext ?: run {
            callback.onError(announcement.utteranceId, "middleware not attached")
            return
        }

        val trace = SharedPerformance.newTrace(TraceName.TEXT_TO_SPEECH_STARTED).start()
        playingJob = ioScope.launch {
            middlewareContext.playInternal(
                announcement = announcement,
                progress = progress ?: VoiceProgress.None,
                fadePlay = true,
                trace = trace,
                callback = callback,
            )
        }
    }

    private suspend fun VoicePlayerMiddlewareContext.playInternal(
        announcement: Announcement,
        progress: VoiceProgress,
        fadePlay: Boolean,
        trace: DashTrace,
        callback: PlayerCallback,
    ) {
        val voiceAnnouncement = prepareVoiceAnnouncement(announcement, progress)
        val traceCallback = PlayerCallbackWrapper(trace, callback)
        val owner = when (voiceAnnouncement) {
            is VoiceAnnouncement.Local -> AudioFocusOwner.TextToSpeech
            is VoiceAnnouncement.Remote -> AudioFocusOwner.MediaPlayer
        }

        audioFocusManager.request(AudioFocusOwner.TextToSpeech) { isGranted ->
            if (isGranted) {
                when (voiceAnnouncement) {
                    is VoiceAnnouncement.Local -> {
                        trace.attribute(TraceKey.SOURCE) { TraceValue.LOCAL }
                        if (fadePlay) {
                            localTTSPlayer.fadePlay(voiceAnnouncement, traceCallback)
                        } else {
                            localTTSPlayer.play(voiceAnnouncement, traceCallback)
                        }
                    }

                    is VoiceAnnouncement.Remote -> {
                        trace.attribute(TraceKey.SOURCE) { TraceValue.REMOTE }
                        if (fadePlay) {
                            remoteTTSPlayer.fadePlay(voiceAnnouncement, traceCallback)
                        } else {
                            remoteTTSPlayer.play(voiceAnnouncement, traceCallback)
                        }
                    }
                }
            } else {
                com.mapbox.navigation.mapgpt.core.common.SharedLog.e(com.mapbox.navigation.mapgpt.core.textplayer.DefaultVoicePlayerMiddleware.TAG) { "Failed to request audio focus for $owner" }
                traceCallback.onComplete(announcement.text, voiceAnnouncement.utteranceId)
            }
        }
    }

    private suspend fun VoicePlayerMiddlewareContext.prepareVoiceAnnouncement(
        announcement: Announcement,
        voiceProgress: VoiceProgress,
    ): VoiceAnnouncement {
        com.mapbox.navigation.mapgpt.core.common.SharedLog.d(com.mapbox.navigation.mapgpt.core.textplayer.DefaultVoicePlayerMiddleware.TAG) { "prepareVoiceAnnouncement $announcement, voiceProgress: $voiceProgress" }

        val measure = announcement.measureDownload()
        fun createLocalAnnouncement() = com.mapbox.navigation.mapgpt.core.textplayer.VoiceAnnouncement.Local(
            announcement.utteranceId,
            announcement.mediaCacheId,
            announcement.text,
            voiceProgress as? VoiceProgress.Index,
        )
        return if (preferLocalTts.value || remoteTTSPlayer.provider == null) {
            measure.log { "use local" }
            createLocalAnnouncement()
        } else {
            measure.log { "request remote tts player" }
            val filePathResult = remoteTTSPlayer.prepare(announcement)
            filePathResult.fold(
                onSuccess = { filePath ->
                    measure.log { "remote tts player success" }
                    com.mapbox.navigation.mapgpt.core.textplayer.VoiceAnnouncement.Remote(
                        announcement.utteranceId,
                        announcement.mediaCacheId,
                        announcement.text,
                        filePath,
                        voiceProgress as? VoiceProgress.Time,
                    )
                },
                onFailure = {
                    measure.log { "error: fallback to local ${it.obfuscateStackTrace()}" }
                    createLocalAnnouncement()
                },
            )
        }
    }

    override fun stop() {
        playingJob?.cancel()
        localTTSPlayer.stop()
        remoteTTSPlayer.stop()
    }

    override suspend fun fadeStop() {
        localTTSPlayer.fadeStop()
        remoteTTSPlayer.fadeStop()
        playingJob?.cancel()
    }

    override fun release() {
        remoteTTSPlayer.release()
    }

    override fun volume(level: Float) {
        if (level in 0.0f..1.0f) {
            localTTSPlayer.volume(level)
            remoteTTSPlayer.volume(level)
        }
    }

    private fun VoicePlayerMiddlewareContext.bindVoiceCapabilities() {
        _availableLanguages.bindAvailability(
            preferLocalTts,
            localTTSPlayer.availableLanguages,
            remoteTTSPlayer.availableLanguages,
        )
        _availableVoices.bindAvailability(
            preferLocalTts,
            localTTSPlayer.availableVoices,
            remoteTTSPlayer.availableVoices,
        )
    }

    private fun <T> MutableStateFlow<T>.bindAvailability(
        isLocalFlow: Flow<Boolean>,
        localFlow: Flow<T>,
        remoteFlow: Flow<T>,
    ) {
        combine(isLocalFlow, localFlow, remoteFlow) { isLocal, local, remote ->
            value = if (isLocal) local else remote
        }.launchIn(ioScope)
    }

    /**
     * Wrap the callback to measure the playback, and keep track of the state.
     */
    inner class PlayerCallbackWrapper(
        private val trace: DashTrace,
        private val callback: PlayerCallback,
    ) : PlayerCallback {

        override fun onStartPlaying(text: String?, utteranceId: String) {
            trace.stop { TraceValue.SUCCESS }
            callback.onStartPlaying(text, utteranceId)
        }

        override fun onComplete(text: String?, utteranceId: String) {
            trace.stop()
            callback.onComplete(text, utteranceId)
        }

        override fun onStop(utteranceId: String, progress: VoiceProgress) {
            trace.stop { TraceValue.CANCELED }
            callback.onStop(utteranceId, progress)
        }

        override fun onError(utteranceId: String, reason: String?) {
            trace.stop { TraceValue.ERROR }
            callback.onError(utteranceId, reason)
        }
    }

    private companion object {

        private const val TAG = "DefaultVoicePlayerMiddleware"
    }
}
