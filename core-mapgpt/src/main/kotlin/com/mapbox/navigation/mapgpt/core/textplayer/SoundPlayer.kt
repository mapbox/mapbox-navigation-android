package com.mapbox.navigation.mapgpt.core.textplayer

interface SoundPlayer {
    suspend fun playSound(sound: Sound)
    fun release()
}

sealed class Sound {
    object StartInterruptionSound : Sound()
    object StopInterruptionSound : Sound()

    /**
     * @param assetFileName Audio file name, including extension, to be found in the platform assets.
     */
    data class CustomSound(val assetFileName: String) : Sound()
}
