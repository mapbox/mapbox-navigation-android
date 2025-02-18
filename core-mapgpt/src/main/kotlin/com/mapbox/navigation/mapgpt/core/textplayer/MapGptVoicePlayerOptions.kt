package com.mapbox.navigation.mapgpt.core.textplayer

/**
 * Options used to initialize the default [VoicePlayer] instance.
 *
 * @param accessToken a Mapbox Access token
 */
data class MapGptVoicePlayerOptions constructor(
    val accessToken: String,
    val voices: Set<DashVoice>,
)
