package com.mapbox.navigation.mapgpt.core.music

import kotlinx.coroutines.flow.StateFlow

interface MusicPlayer {
    val provider: MusicPlayerProvider

    val playbackState: StateFlow<MusicPlayerState?>

    /**
     * @param providerUri is defined by Mapbox which is specific to the [provider].
     */
    fun play(providerUri: String)
    fun pause()
    fun resume()
    fun stop()
    fun next()
    fun previous()
    fun setShuffleMode(mode: MusicPlayerState.ShuffleMode)
    fun setRepeatMode(mode: MusicPlayerState.RepeatMode)
    fun seek(position: Float)
}
