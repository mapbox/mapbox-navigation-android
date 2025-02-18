package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.MiddlewareContext
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.config.api.RemoteTtsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RemoteTTSPlayerSwitcher : RemoteTTSPlayer, CoroutineMiddleware<MiddlewareContext>() {
    private val _remoteTTSPlayer = MutableStateFlow<RemoteTTSPlayer?>(null)
    val remoteTTSPlayer: StateFlow<RemoteTTSPlayer?> = _remoteTTSPlayer.asStateFlow()

    @RemoteTtsProvider
    override val provider:  String?
        get() = remoteTTSPlayer.value?.provider

    private val _availableLanguages = MutableStateFlow<Set<Language>>(emptySet())
    override val availableLanguages: StateFlow<Set<Language>> = _availableLanguages.asStateFlow()
    private val _availableVoices = MutableStateFlow<Set<Voice>>(emptySet())
    override val availableVoices: StateFlow<Set<Voice>> = _availableVoices.asStateFlow()

    fun setRemoteTTSPlayer(remoteTTSPlayer: RemoteTTSPlayer) {
        _remoteTTSPlayer.value = remoteTTSPlayer
    }

    override fun onAttached(middlewareContext: MiddlewareContext) {
        super.onAttached(middlewareContext)
        SharedLog.d(TAG) { "onAttached" }
        var currentTts = remoteTTSPlayer.value
        ioScope.launch {
            remoteTTSPlayer.collectLatest { nextTts ->
                currentTts?.stop()
                currentTts?.release()
                currentTts = nextTts
            }
        }
        launchStateObservers()
    }

    override fun onDetached(middlewareContext: MiddlewareContext) {
        super.onDetached(middlewareContext)
        remoteTTSPlayer.value.apply {
            stop()
            release()
        }
        _availableLanguages.value = emptySet()
        _availableVoices.value = emptySet()
        SharedLog.d(TAG) { "onDetached" }
    }

    override fun prefetch(announcement: Announcement) {
        runOnPlayer("prefetch $announcement") { prefetch(announcement) }
    }

    override suspend fun prepare(announcement: Announcement): Result<String> {
        SharedLog.d(TAG) { "$provider prepare $announcement" }
        return remoteTTSPlayer.value?.prepare(announcement)
            ?: Result.failure(RuntimeException("RemoteTtsProvider is not set"))
    }

    override fun play(voice: VoiceAnnouncement.Remote, callback: PlayerCallback) {
        runOnPlayer("play($voice)") { play(voice, callback) }
    }

    override fun stop() {
        runOnPlayer("stop") { stop() }
    }

    override fun volume(level: Float) {
        runOnPlayer("volume($level") { volume(level) }
    }

    override fun release() {
        runOnPlayer("release") { release() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun launchStateObservers() {
        merge(
            remoteTTSPlayer.onEach {
                SharedLog.d(TAG) { "Switching to ${it?.provider}" }
            },
            remoteTTSPlayer.flatMapLatest { it?.availableLanguages ?: flowOf(emptySet()) }.onEach { languages ->
                _availableLanguages.value = languages
                SharedLog.d(TAG) { "Available languages ${languages.joinToString { it.languageTag }}" }
            },
            remoteTTSPlayer.flatMapLatest { it?.availableVoices ?: flowOf(emptySet())  }.onEach { languages ->
                SharedLog.d(TAG) { "Available voices ${languages.joinToString()}" }
                _availableVoices.value = languages
            },
        ).launchIn(ioScope)
    }

    private fun runOnPlayer(method: String, action: RemoteTTSPlayer.() -> Unit) {
        val player = remoteTTSPlayer.value
        SharedLog.d(TAG) { "${player?.provider} $method" }
        player?.action()
    }

    private companion object {
        private const val TAG = "RemoteTTSPlayerSwitcher"
    }
}
