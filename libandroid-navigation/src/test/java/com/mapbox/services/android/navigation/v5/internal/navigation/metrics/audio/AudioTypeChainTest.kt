package com.mapbox.services.android.navigation.v5.internal.navigation.metrics.audio

import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test

class AudioTypeChainTest {

    @Test
    fun checkChainIsCorrectWhenSetupCalled() {
        val audioTypeChain = AudioTypeChain()
        val unknownAudioType = mockk<UnknownAudioType>(relaxed = true)
        val speakerAudioType = mockk<SpeakerAudioType>(relaxed = true)
        val headphonesAudioType = mockk<HeadphonesAudioType>(relaxed = true)
        val bluetoothAudioType = mockk<BluetoothAudioType>(relaxed = true)

        audioTypeChain.setup(
            unknownAudioType,
            speakerAudioType,
            headphonesAudioType,
            bluetoothAudioType
        )

        verifyOrder {
            speakerAudioType.nextChain(eq(unknownAudioType))
            headphonesAudioType.nextChain(eq(speakerAudioType))
            bluetoothAudioType.nextChain(eq(headphonesAudioType))
        }
    }
}
