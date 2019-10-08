package com.mapbox.services.android.navigation.v5.internal.navigation.metrics.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.mapbox.services.android.navigation.v5.internal.exception.NavigationException

sealed class AudioTypeResolver {

    companion object {
        const val BLUETOOTH = "bluetooth"
        const val HEADPHONES = "headphones"
        const val SPEAKER = "speaker"
        const val UNKNOWN = "unknown"
    }

    var chain: AudioTypeResolver? = null

    open fun nextChain(chain: AudioTypeResolver) {
        this.chain = chain
    }

    abstract fun obtainAudioType(context: Context): String
}

class BluetoothAudioType : AudioTypeResolver() {

    override fun obtainAudioType(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            ?: return UNKNOWN
        return if (audioManager.isBluetoothScoOn) {
            BLUETOOTH
        } else {
            chain?.obtainAudioType(context)
                ?: throw NavigationException(
                    "Invalid chain for AudioType: next element for BluetoothAudioType didn't set"
                )
        }
    }
}

class HeadphonesAudioType : AudioTypeResolver() {

    override fun obtainAudioType(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            ?: return UNKNOWN
        val isHeadphonesOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.isNotEmpty()
        } else {
            audioManager.isWiredHeadsetOn
        }
        return if (isHeadphonesOn) {
            HEADPHONES
        } else {
            chain?.obtainAudioType(context)
                ?: throw NavigationException(
                    "Invalid chain for AudioType: next element for HeadphonesAudioType didn't set"
                )
        }
    }
}

class SpeakerAudioType : AudioTypeResolver() {

    override fun obtainAudioType(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            ?: return UNKNOWN
        return if (audioManager.isSpeakerphoneOn) {
            SPEAKER
        } else {
            chain?.obtainAudioType(context)
                ?: throw NavigationException(
                    "Invalid chain for AudioType: next element for SpeakerAudioType didn't set"
                )
        }
    }
}

class UnknownAudioType : AudioTypeResolver() {

    override fun nextChain(chain: AudioTypeResolver) {
    }

    override fun obtainAudioType(context: Context): String = UNKNOWN
}
