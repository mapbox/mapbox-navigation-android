package com.mapbox.navigation.mapgpt.core.textplayer.middleware

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.config.api.RemoteTtsProvider
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.textplayer.NotificationAlert
import com.mapbox.navigation.mapgpt.core.textplayer.Announcement
import com.mapbox.navigation.mapgpt.core.textplayer.AudioMixer
import com.mapbox.navigation.mapgpt.core.textplayer.DefaultVoicePlayerMiddleware
import com.mapbox.navigation.mapgpt.core.textplayer.Player
import com.mapbox.navigation.mapgpt.core.textplayer.PlatformAudioPlayerFactory
import com.mapbox.navigation.mapgpt.core.textplayer.PlayerState
import com.mapbox.navigation.mapgpt.core.textplayer.RemoteTTSPlayerSwitcher
import com.mapbox.navigation.mapgpt.core.textplayer.VoicePlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class PlayerManager(
    private val playerFactory: PlatformAudioPlayerFactory,
) : Player, CoroutineMiddleware<MapGptCoreContext>() {

    private val voicePlayerContext = playerFactory.createContext()
    private val defaultVoicePlayer: DefaultVoicePlayerMiddleware = DefaultVoicePlayerMiddleware(
        localTTSPlayer = playerFactory.createLocalTTSPlayer(),
        remoteTTSPlayer = RemoteTTSPlayerSwitcher(),
    )
    private val switcher = VoicePlayerMiddlewareSwitcher(defaultVoicePlayer)
    private val audioMixer = AudioMixerMiddleware(switcher)

    val voicePlayer: VoicePlayer = switcher

    override val state: StateFlow<PlayerState> = audioMixer.playerState

    override var preferLocalTts: Boolean = voicePlayerContext.preferLocalTts.value
        get() = voicePlayerContext.preferLocalTts.value
        set(value) {
            voicePlayerContext.setPreferLocalTts(value)
            field = value
        }

    override var isMuted: Boolean = voicePlayerContext.isMuted.value
        get() = voicePlayerContext.isMuted.value
        set(value) {
            voicePlayerContext.setIsMuted(value)
            field = value
        }

    override var playPriorityExclusively: Boolean = voicePlayerContext.playPriorityExclusively.value
        get() = voicePlayerContext.playPriorityExclusively.value
        set(value) {
            voicePlayerContext.setPlayPriorityExclusively(value)
            field = value
        }


    override fun onAttached(middlewareContext: MapGptCoreContext) {
        super.onAttached(middlewareContext)
        switcher.onAttached(voicePlayerContext)
        audioMixer.onAttached(voicePlayerContext)
    }

    override fun onDetached(middlewareContext: MapGptCoreContext) {
        super.onDetached(middlewareContext)
        audioMixer.detach()
    }

    /**
     * Set the default Speech to Text middleware.
     */
    fun setDefaultVoicePlayerMiddleware() {
        switcher.unregisterMiddleware()
    }

    /**
     * Set the Text to Speech middleware.
     * There can only be one [VoicePlayerMiddleware] at a time.
     */
    fun setVoicePlayerMiddleware(middleware: VoicePlayerMiddleware) {
        switcher.registerMiddleware(middleware)
    }

    fun setRemoteTtsProvider(@RemoteTtsProvider remoteTtsProvider: String) {
        val remoteTTSPlayer = playerFactory.createRemoteTTSPlayer(remoteTtsProvider)
        defaultVoicePlayer.remoteTTSPlayer.setRemoteTTSPlayer(remoteTTSPlayer)
    }

    override fun prefetch(announcement: Announcement) {
        SharedLog.d(TAG) { "prefetch: $announcement" }
        voicePlayer.prefetch(announcement)
    }

    override fun play(announcement: Announcement) {
        SharedLog.d(TAG) { "play added to queue: $announcement" }
        audioMixer.insert(announcement)
    }

    override fun play(notificationAlert: NotificationAlert) {
        SharedLog.d(TAG) { "alert added to queue: $notificationAlert" }
        audioMixer.insert(notificationAlert)
    }

    override fun clear() {
        SharedLog.d(TAG) { "clear" }
        audioMixer.removeAll()
    }

    override fun clearRegularQueue() {
        SharedLog.d(TAG) { "clearQueueLane ${AudioMixer.TRACK_REGULAR}" }
        audioMixer.removeTrack(AudioMixer.TRACK_REGULAR)
    }

    override fun clearPriorityQueue() {
        SharedLog.d(TAG) { "clearQueueLane ${AudioMixer.TRACK_PRIORITY}" }
        audioMixer.removeTrack(AudioMixer.TRACK_PRIORITY)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun prefetchedAnnouncementsCleared(): Flow<Unit> {
        return switcher.middlewareState.flatMapLatest { middleware ->
            when (middleware) {
                is DefaultVoicePlayerMiddleware -> {
                    middleware.remoteTTSPlayer.remoteTTSPlayer.map { Unit }
                }
                else -> flowOf(Unit)
            }
        }.onEach {
            SharedLog.w(TAG) { "Clearing audio after player change" }
            audioMixer.removeAll()
        }
    }

    private companion object {
        private const val TAG = "PlayerManager"
    }
}
