package com.mapbox.navigation.mapgpt.core.music

import com.mapbox.navigation.mapgpt.core.MiddlewareContext
import kotlinx.coroutines.flow.StateFlow

interface MusicPlayerContext : MiddlewareContext {
    val playerState: StateFlow<MusicPlayerState?>
    val musicPlayer: MusicPlayer
}
