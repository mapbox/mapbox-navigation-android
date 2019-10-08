package com.mapbox.services.android.navigation.v5.internal.navigation.metrics.audio

class AudioTypeChain {

    @JvmOverloads
    fun setup(
        unknownAudioType: UnknownAudioType = UnknownAudioType(),
        speakerAudioType: SpeakerAudioType = SpeakerAudioType(),
        headphonesAudioType: HeadphonesAudioType = HeadphonesAudioType(),
        bluetoothAudioType: BluetoothAudioType = BluetoothAudioType()
    ): AudioTypeResolver {
        speakerAudioType.nextChain(unknownAudioType)
        headphonesAudioType.nextChain(speakerAudioType)
        bluetoothAudioType.nextChain(headphonesAudioType)
        return bluetoothAudioType
    }
}
