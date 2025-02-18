package com.mapbox.navigation.mapgpt.core.music

import com.mapbox.navigation.mapgpt.core.MiddlewareProvider

sealed class MusicPlayerProvider(key: String): MiddlewareProvider(key) {
    object None : MusicPlayerProvider("")
    object Apple : MusicPlayerProvider("applemusic")
    object Spotify : MusicPlayerProvider("spotify")
}
