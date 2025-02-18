package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.language.Language
import kotlinx.coroutines.flow.StateFlow

interface LocalTTSPlayer {

    val availableLanguages: StateFlow<Set<Language>>
    val availableVoices: StateFlow<Set<Voice>>

    fun play(
        voice: VoiceAnnouncement.Local,
        callback: PlayerCallback,
    )

    fun fadePlay(
        voice: VoiceAnnouncement.Local,
        callback: PlayerCallback,
    ) = play(voice, callback)

    fun stop()

    suspend fun fadeStop() = stop()

    fun volume(level: Float)
}
