package com.mapbox.navigation.mapgpt.core.textplayer

interface SpeechFilePlayer {

    fun play(
        remoteAnnouncement: VoiceAnnouncement.Remote,
        callback: PlayerCallback,
    )

    fun fadePlay(
        remoteAnnouncement: VoiceAnnouncement.Remote,
        callback: PlayerCallback,
    ) = play(remoteAnnouncement, callback)

    fun stop()

    suspend fun fadeStop() = stop()

    fun volume(level: Float)

    fun release()
}
