package com.mapbox.navigation.core.telemetry.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build

sealed class AudioTypeResolver {

    companion object {
        const val BLUETOOTH = "bluetooth"
        const val HEADPHONES = "headphones"
        const val SPEAKER = "speaker"
        const val UNKNOWN = "unknown"
    }

    internal lateinit var chain: AudioTypeResolver

    open fun nextChain(chain: AudioTypeResolver) {
        this.chain = chain
    }

    abstract fun obtainAudioType(context: Context): String

    class Bluetooth : AudioTypeResolver() {
        override fun obtainAudioType(context: Context): String {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                ?: return UNKNOWN
            return if (audioManager.isBluetoothScoOn) {
                BLUETOOTH
            } else {
                chain.obtainAudioType(context)
            }
        }
    }

    class Headphones : AudioTypeResolver() {
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
                chain.obtainAudioType(context)
            }
        }
    }

    class Speaker : AudioTypeResolver() {
        override fun obtainAudioType(context: Context): String {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                ?: return UNKNOWN
            return if (audioManager.isSpeakerphoneOn) {
                SPEAKER
            } else {
                chain.obtainAudioType(context)
            }
        }
    }

    class Unknown : AudioTypeResolver() {
        override fun nextChain(chain: AudioTypeResolver) {
        }

        override fun obtainAudioType(context: Context): String = UNKNOWN
    }
}
