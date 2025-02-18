package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.config.api.RemoteTtsProvider
import kotlinx.coroutines.flow.StateFlow

interface RemoteTTSPlayer {
    @RemoteTtsProvider
    val provider: String?

    val availableLanguages: StateFlow<Set<Language>>
    val availableVoices: StateFlow<Set<Voice>>

    fun prefetch(announcement: Announcement)

    suspend fun prepare(announcement: Announcement): Result<String>

    fun play(
        voice: VoiceAnnouncement.Remote,
        callback: PlayerCallback,
    )

    fun fadePlay(
        voice: VoiceAnnouncement.Remote,
        callback: PlayerCallback,
    ) = play(voice, callback)

    fun stop()

    suspend fun fadeStop() = stop()

    fun volume(level: Float)

    fun release()
}
