package com.mapbox.navigation.core.telemetry.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.mapbox.navigator.AudioType

internal sealed class AudioTypeResolver {

    internal lateinit var chain: AudioTypeResolver

    fun toNativeAudioType(): AudioType = when (this) {
        is Bluetooth -> AudioType.BLUETOOTH
        is Speaker -> AudioType.SPEAKER
        is Headphones -> AudioType.HEADPHONES
        is Unknown -> AudioType.UNKNOWN
    }

    open fun nextChain(chain: AudioTypeResolver) {
        this.chain = chain
    }

    abstract fun obtainAudioType(context: Context): AudioTypeResolver

    class Bluetooth : AudioTypeResolver() {
        override fun obtainAudioType(context: Context): AudioTypeResolver {
            return if (context.audioManager?.isBluetoothScoOn == true) {
                Bluetooth()
            } else {
                chain.obtainAudioType(context)
            }
        }
    }

    class Headphones : AudioTypeResolver() {
        override fun obtainAudioType(context: Context): AudioTypeResolver {
            val audioManager = context.audioManager ?: return Unknown()
            val isHeadphonesOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.isNotEmpty()
            } else {
                audioManager.isWiredHeadsetOn
            }
            return if (isHeadphonesOn) {
                Headphones()
            } else {
                chain.obtainAudioType(context)
            }
        }
    }

    class Speaker : AudioTypeResolver() {
        override fun obtainAudioType(context: Context): AudioTypeResolver {
            return if (context.audioManager?.isSpeakerphoneOn == true) {
                Speaker()
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

    private companion object {
        val Context.audioManager: AudioManager?
            get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    }
}
