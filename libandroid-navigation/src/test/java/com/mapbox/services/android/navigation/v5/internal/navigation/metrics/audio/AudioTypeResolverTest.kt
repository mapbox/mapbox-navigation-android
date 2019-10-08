package com.mapbox.services.android.navigation.v5.internal.navigation.metrics.audio

import android.content.Context
import android.media.AudioManager
import com.mapbox.services.android.navigation.v5.internal.exception.NavigationException
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class AudioTypeResolverTest {

    @get:Rule
    val thrown: ExpectedException = ExpectedException.none()

    @Test
    fun checkSpeakersAudioTypeThrowsExceptionWhenChainSsNull() {
        thrown.expect(NavigationException::class.java)
        thrown.expectMessage("Invalid chain for AudioType: next element for SpeakerAudioType didn't set")
        val speakerAudioType = SpeakerAudioType()
        val audioManager = mockk<AudioManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every {
                getSystemService(Context.AUDIO_SERVICE)
            } returns audioManager
        }

        speakerAudioType.obtainAudioType(context)
    }

    @Test
    fun checkHeadphonesAudioTypeThrowsExceptionWhenChainSsNull() {
        thrown.expect(NavigationException::class.java)
        thrown.expectMessage("Invalid chain for AudioType: next element for HeadphonesAudioType didn't set")
        val headphonesAudioType = HeadphonesAudioType()
        val audioManager = mockk<AudioManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every {
                getSystemService(Context.AUDIO_SERVICE)
            } returns audioManager
        }

        headphonesAudioType.obtainAudioType(context)
    }

    @Test
    fun checkBluetoothAudioTypeThrowsExceptionWhenChainSsNull() {
        thrown.expect(NavigationException::class.java)
        thrown.expectMessage("Invalid chain for AudioType: next element for BluetoothAudioType didn't set")
        val bluetoothAudioType = BluetoothAudioType()
        val audioManager = mockk<AudioManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every {
                getSystemService(Context.AUDIO_SERVICE)
            } returns audioManager
        }

        bluetoothAudioType.obtainAudioType(context)
    }
}
