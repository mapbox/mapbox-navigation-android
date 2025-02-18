package com.mapbox.navigation.mapgpt.core.textplayer

internal class PendingSounds : AudioMixer.Clip {
    override val track: Int = AudioMixer.TRACK_PRIORITY

    override fun toString(): String {
        return "PendingSounds"
    }

    companion object {
        const val TAG = "PendingSounds"

        const val PENDING_SOUND_FILE: String = "pending-sound.mp3"
    }
}
