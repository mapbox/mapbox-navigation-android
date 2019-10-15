package com.mapbox.services.android.navigation.v5.internal.navigation.metrics.audio

internal class AudioTypeChain {

    fun setup(
        unknownAudioType: AudioTypeResolver.Unknown = AudioTypeResolver.Unknown(),
        speakerAudioType: AudioTypeResolver.Speaker = AudioTypeResolver.Speaker(),
        headphonesAudioType: AudioTypeResolver.Headphones = AudioTypeResolver.Headphones(),
        bluetoothAudioType: AudioTypeResolver.Bluetooth = AudioTypeResolver.Bluetooth()
    ): AudioTypeResolver {
        speakerAudioType.nextChain(unknownAudioType)
        headphonesAudioType.nextChain(speakerAudioType)
        bluetoothAudioType.nextChain(headphonesAudioType)
        return bluetoothAudioType
    }
}
