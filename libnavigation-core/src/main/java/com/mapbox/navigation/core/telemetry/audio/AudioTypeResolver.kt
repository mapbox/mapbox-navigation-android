package com.mapbox.navigation.core.telemetry.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build

internal sealed class AudioTypeResolver {

    internal lateinit var chain: AudioTypeResolver

    open fun nextChain(chain: AudioTypeResolver) {
        this.chain = chain
    }

    abstract fun obtainAudioType(context: Context): AudioTypeResolver

    class Bluetooth : AudioTypeResolver() {
        override fun obtainAudioType(context: Context): AudioTypeResolver {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                ?: return Unknown()
            return if (audioManager.isBluetoothScoOn) {
                this
            } else {
                chain.obtainAudioType(context)
            }
        }
    }

    class Headphones : AudioTypeResolver() {
        override fun obtainAudioType(context: Context): AudioTypeResolver {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                ?: return Unknown()
            val isHeadphonesOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.isNotEmpty()
            } else {
                audioManager.isWiredHeadsetOn
            }
            return if (isHeadphonesOn) {
                this
            } else {
                chain.obtainAudioType(context)
            }
        }
    }

    class Speaker : AudioTypeResolver() {
        override fun obtainAudioType(context: Context): AudioTypeResolver {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                ?: return Unknown()
            return if (audioManager.isSpeakerphoneOn) {
                this
            } else {
                chain.obtainAudioType(context)
            }
        }
    }

    class Unknown : AudioTypeResolver() {
        override fun nextChain(chain: AudioTypeResolver) {
        }

        override fun obtainAudioType(context: Context): AudioTypeResolver = this
    }
}
