package com.mapbox.navigation.mapgpt.core.textplayer.middleware

import com.mapbox.navigation.mapgpt.core.MiddlewareSwitcher
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.textplayer.Announcement
import com.mapbox.navigation.mapgpt.core.textplayer.PlayerCallback
import com.mapbox.navigation.mapgpt.core.textplayer.Voice
import com.mapbox.navigation.mapgpt.core.textplayer.VoicePlayer
import com.mapbox.navigation.mapgpt.core.textplayer.VoiceProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class VoicePlayerMiddlewareSwitcher(
    default: VoicePlayerMiddleware,
) : MiddlewareSwitcher<VoicePlayerMiddlewareContext, VoicePlayerMiddleware>(default), VoicePlayer {
    private val _availableLanguages = MutableStateFlow(default.availableLanguages.value)
    override val availableLanguages: StateFlow<Set<Language>> = _availableLanguages
    private val _availableVoices = MutableStateFlow(default.availableVoices.value)
    override val availableVoices: StateFlow<Set<Voice>> = _availableVoices

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onAttached(middlewareContext: VoicePlayerMiddlewareContext) {
        super.onAttached(middlewareContext)
        middlewareState.flatMapLatest { it.availableLanguages }.onEach { availableLanguages ->
            _availableLanguages.value = availableLanguages
        }.launchIn(mainScope)
        middlewareState.flatMapLatest { it.availableVoices }.onEach { availableVoices ->
            _availableVoices.value = availableVoices
        }.launchIn(mainScope)
    }

    override fun prefetch(announcement: Announcement) {
        middlewareState.value.prefetch(announcement)
    }

    override fun play(
        announcement: Announcement,
        progress: VoiceProgress?,
        callback: PlayerCallback,
    ) {
        middlewareState.value.play(announcement, progress, callback)
    }

    override fun fadePlay(
        announcement: Announcement,
        progress: VoiceProgress?,
        callback: PlayerCallback
    ) {
        middlewareState.value.fadePlay(announcement, progress, callback)
    }

    override fun stop() {
        middlewareState.value.stop()
    }

    override suspend fun fadeStop() {
        middlewareState.value.fadeStop()
    }

    override fun release() {
        middlewareState.value.release()
    }

    override fun volume(level: Float) {
        middlewareState.value.volume(level)
    }
}
