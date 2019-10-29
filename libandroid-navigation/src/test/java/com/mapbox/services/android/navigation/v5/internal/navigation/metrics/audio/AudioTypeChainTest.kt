package com.mapbox.services.android.navigation.v5.internal.navigation.metrics.audio

import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test

class AudioTypeChainTest {

    @Test
    fun checksChainIsCorrectWhenSetupCalled() {
        val audioTypeChain =
            AudioTypeChain()
        val unknownAudioType = mockk<AudioTypeResolver.Unknown>(relaxed = true)
        val speakerAudioType = mockk<AudioTypeResolver.Speaker>(relaxed = true)
        val headphonesAudioType = mockk<AudioTypeResolver.Headphones>(relaxed = true)
        val bluetoothAudioType = mockk<AudioTypeResolver.Bluetooth>(relaxed = true)

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
