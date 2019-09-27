package com.mapbox.services.android.navigation.v5.navigation.metrics.audio

import android.content.Context
import android.media.AudioManager

internal class HeadphonesAudioType : AudioTypeResolver {
    private val HEADPHONES = "headphones"
    private var chain: AudioTypeResolver? = null

    override fun nextChain(chain: AudioTypeResolver) {
        this.chain = chain
    }

    override fun obtainAudioType(context: Context): String {
        val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager?.let { audio ->
            when (audio.isWiredHeadsetOn) {
                true -> HEADPHONES
                false -> {
                    chain?.obtainAudioType(context) ?: "unknown"
                }
            }
        } ?: "unknown"
    }
}
