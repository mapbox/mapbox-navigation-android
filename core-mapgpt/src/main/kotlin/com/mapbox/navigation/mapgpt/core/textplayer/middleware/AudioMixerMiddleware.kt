package com.mapbox.navigation.mapgpt.core.textplayer.middleware

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.audiofocus.AudioFocusOwner
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.textplayer.NotificationAlert
import com.mapbox.navigation.mapgpt.core.textplayer.Announcement
import com.mapbox.navigation.mapgpt.core.textplayer.AudioMixer
import com.mapbox.navigation.mapgpt.core.textplayer.AudioMixerImpl
import com.mapbox.navigation.mapgpt.core.textplayer.PendingSounds
import com.mapbox.navigation.mapgpt.core.textplayer.PlayerCallback
import com.mapbox.navigation.mapgpt.core.textplayer.PlayerState
import com.mapbox.navigation.mapgpt.core.textplayer.Sound
import com.mapbox.navigation.mapgpt.core.textplayer.VoicePlayer
import com.mapbox.navigation.mapgpt.core.textplayer.VoiceProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation

/**
 * Audio clips can be played as announcements or sounds, or effect the playback of clips.
 * This middleware manages the playback of audio clips and their interruptions.
 */
class AudioMixerMiddleware(
    private val voicePlayer: VoicePlayer,
) : CoroutineMiddleware<VoicePlayerMiddlewareContext>() {

    private val audioMixer = AudioMixerImpl()
    private val voiceProgressMap = mutableMapOf<String, VoiceProgress>()

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private fun setPlayerState(state: PlayerState) {
        _playerState.value = state
    }

    override fun onAttached(middlewareContext: VoicePlayerMiddlewareContext) {
        super.onAttached(middlewareContext)
        SharedLog.d(TAG) { "onAttached" }

        playerState.onEach { state ->
            SharedLog.d(TAG) { "playerState: $state" }
        }.launchIn(ioScope)
        middlewareContext.playPriorityExclusively.onEach { playPriorityExclusively ->
            SharedLog.d(TAG) { "playPriorityExclusively: $playPriorityExclusively" }
        }.launchIn(ioScope)

        mainScope.launch {
            audioMixer.current.collectLatest { clip ->
                SharedLog.d(TAG) { "onNextAudioClip $clip" }
                val isCompleted = middlewareContext.onNextAudioClip(clip)
                if (clip != null && isCompleted) {
                    SharedLog.d(TAG) { "onNextAudioClip removeClip $clip" }
                    audioMixer.removeClip(clip)
                    if (shouldPlayPendingSounds()) {
                        audioMixer.insert(PendingSounds(), interrupts = false)
                    }
                    audioMixer.updateCurrent()
                }
            }
        }

        middlewareContext.isMuted.onEach { isMuted ->
            if (isMuted) {
                voicePlayer.stop()
                audioMixer.removeAll()
                audioMixer.updateCurrent()
            }
        }.launchIn(mainScope)
    }

    override fun onDetached(middlewareContext: VoicePlayerMiddlewareContext) {
        super.onDetached(middlewareContext)
        audioMixer.removeAll()
        audioMixer.updateCurrent()
        voiceProgressMap.clear()
        SharedLog.d(TAG) { "onDetached" }
    }

    fun removeAll() {
        audioMixer.removeAll()
        audioMixer.updateCurrent()
    }

    fun insert(announcement: Announcement) {
        val isMuted = middlewareContext?.isMuted?.value == true
        if (isMuted || announcement.text.isBlank()) {
            SharedLog.d(TAG) { "ignore announcement isMuted: $isMuted, announcement: $announcement" }
            return
        }
        voicePlayer.prefetch(announcement)
        audioMixer.insert(
            clip = announcement,
            interrupts = announcement is Announcement.Priority,
        )
        audioMixer.updateCurrent()
    }

    fun insert(notificationAlert: NotificationAlert) {
        val isMuted = middlewareContext?.isMuted?.value == true
        if (isMuted) {
            SharedLog.d(TAG) { "ignore alert isMuted: $isMuted, alert: $notificationAlert" }
            return
        }
        audioMixer.insert(
            clip = notificationAlert,
            interrupts = notificationAlert is NotificationAlert.RoadCamera,
        )
        audioMixer.updateCurrent()
    }

    fun removeTrack(lane: Int) {
        audioMixer.removeTrack(lane)
        audioMixer.updateCurrent()
    }

    /**
     * Play the next audio clip in the queue. Returns true if the clip should be removed from the
     * queue.
     */
    private suspend fun VoicePlayerMiddlewareContext.onNextAudioClip(
        audioClip: AudioMixer.Clip?,
    ): Boolean {
        return when (audioClip) {
            is Announcement.Regular -> {
                voicePlayer.stop()
                playAnnouncement(audioClip)
            }

            is Announcement.Priority -> {
                voicePlayer.stop()
                playAnnouncement(audioClip)
            }

            is PendingSounds -> {
                voicePlayer.stop()
                while (shouldPlayPendingSounds()) {
                    SharedLog.d(TAG) { "play pending sound ${audioMixer.clips(AudioMixer.TRACK_PRIORITY)}" }
                    soundPlayer.playSound(Sound.CustomSound(PendingSounds.PENDING_SOUND_FILE))
                }
                true
            }

            is AudioMixer.InterruptionStart -> {
                voicePlayer.fadeStop()
                soundPlayer.playSound(Sound.StartInterruptionSound)
                val shouldRemove = onNextAudioClip(audioClip.priority)
                soundPlayer.playSound(Sound.StopInterruptionSound)
                shouldRemove
            }

            is AudioMixer.InterruptionEnd -> {
                when (audioClip.deferred) {
                    is Announcement -> {
                        voicePlayer.stop()
                        fadePlayAnnouncement(audioClip.deferred)
                    }

                    else -> {
                        onNextAudioClip(audioClip.deferred)
                    }
                }
            }

            is NotificationAlert.RoadCamera -> {
                soundPlayer.playSound(Sound.CustomSound(audioClip.soundFile))
                true
            }

            is NotificationAlert.Incident -> {
                soundPlayer.playSound(Sound.CustomSound(audioClip.soundFile))
                true
            }

            null -> {
                abandonAudioFocus()
                setPlayerState(PlayerState.Idle)
                false
            }
        }
    }

    /**
     * Abandon audio focus for the audio focus owners when the player is done playing.
     */
    private fun VoicePlayerMiddlewareContext.abandonAudioFocus() {
        fun abandonFocusWithLog(owner: AudioFocusOwner) {
            audioFocusManager.abandon(owner) { result ->
                SharedLog.d(TAG) { "AbandonAudioFocus $owner -> $result" }
            }
        }
        abandonFocusWithLog(AudioFocusOwner.TextToSpeech)
        abandonFocusWithLog(AudioFocusOwner.MediaPlayer)
    }

    private suspend fun playAnnouncement(
        announcement: Announcement,
    ): Boolean {
        setPlayerState(PlayerState.Preparing(announcement.utteranceId))
        return suspendCancellableCoroutine { continuation ->
            voicePlayer.play(
                announcement,
                useVoiceProgress(announcement.mediaCacheId),
                ContinuationPlayerCallback(announcement, ::setPlayerState, continuation),
            )
            continuation.invokeOnCancellation {
                if (!continuation.isCompleted) {
                    continuation.resumeWith(Result.success(playerState.value.isCompleted()))
                }
            }
        }
    }

    private suspend fun fadePlayAnnouncement(
        announcement: Announcement,
    ): Boolean {
        setPlayerState(PlayerState.Preparing(announcement.utteranceId))
        return suspendCancellableCoroutine { continuation ->
            voicePlayer.fadePlay(
                announcement,
                useVoiceProgress(announcement.mediaCacheId),
                ContinuationPlayerCallback(announcement, ::setPlayerState, continuation),
            )
            continuation.invokeOnCancellation {
                if (!continuation.isCompleted) {
                    continuation.resumeWith(Result.success(playerState.value.isCompleted()))
                }
            }
        }
    }

    private fun useVoiceProgress(mediaCacheId: String): VoiceProgress? {
        val voiceProgress = voiceProgressMap.remove(mediaCacheId)
        SharedLog.d(TAG) { "useVoiceProgress: $mediaCacheId, $voiceProgress" }
        return voiceProgress
    }

    private fun shouldPlayPendingSounds(): Boolean {
        return middlewareContext?.playPriorityExclusively?.value == true &&
            audioMixer.clips(AudioMixer.TRACK_REGULAR).isNotEmpty() &&
            audioMixer.clips(AudioMixer.TRACK_PRIORITY).filterIsInstance<Announcement>().isEmpty()
    }

    private fun PlayerState.isCompleted(): Boolean {
        return when (this) {
            is PlayerState.Done,
            is PlayerState.Error,
            -> true

            else -> false
        }
    }

    private inner class ContinuationPlayerCallback(
        private val announcement: Announcement,
        private val setPlayerState: (PlayerState) -> Unit,
        private val continuation: Continuation<Boolean>,
    ) : PlayerCallback {

        override fun onStartPlaying(text: String?, utteranceId: String) {
            SharedLog.d(TAG) { "onStartPlaying: text: $text, utteranceId: $utteranceId" }
            setPlayerState(PlayerState.Speaking(text, utteranceId))
        }

        override fun onComplete(text: String?, utteranceId: String) {
            SharedLog.d(TAG) { "onComplete: text: $text, utteranceId: $utteranceId" }
            val state = PlayerState.Done(text, utteranceId)
            setPlayerState(state)
            continuation.resumeWith(Result.success(true))
        }

        override fun onStop(utteranceId: String, progress: VoiceProgress) {
            SharedLog.d(TAG) { "onStop: $utteranceId, $progress" }
            voiceProgressMap[announcement.mediaCacheId] = progress
            val state = PlayerState.Stopped(progress, announcement.text, utteranceId)
            setPlayerState(state)
            continuation.resumeWith(Result.success(false))
        }

        override fun onError(utteranceId: String, reason: String?) {
            val nonNullReason = reason ?: "playAnnouncement error"
            SharedLog.w(TAG) { "onError: $utteranceId, $nonNullReason" }
            val state = PlayerState.Error(utteranceId, nonNullReason)
            setPlayerState(state)
            continuation.resumeWith(Result.success(true))
        }
    }

    private companion object {

        private const val TAG = "AudioMixerMiddleware"
    }
}
