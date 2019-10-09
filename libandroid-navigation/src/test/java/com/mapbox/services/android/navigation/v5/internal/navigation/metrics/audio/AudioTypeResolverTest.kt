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
    fun checksSpeakersAudioTypeThrowsExceptionWhenChainIsNull() {
        thrown.expect(NavigationException::class.java)
        thrown.expectMessage("Invalid chain for AudioType: next element for Speaker didn't set")
        val speakerAudioType = AudioTypeResolver.Speaker()
        val audioManager = mockk<AudioManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every {
                getSystemService(Context.AUDIO_SERVICE)
            } returns audioManager
        }

        speakerAudioType.obtainAudioType(context)
    }

    @Test
    fun checksHeadphonesAudioTypeThrowsExceptionWhenChainIsNull() {
        thrown.expect(NavigationException::class.java)
        thrown.expectMessage("Invalid chain for AudioType: next element for Headphones didn't set")
        val headphonesAudioType = AudioTypeResolver.Headphones()
        val audioManager = mockk<AudioManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every {
                getSystemService(Context.AUDIO_SERVICE)
            } returns audioManager
        }

        headphonesAudioType.obtainAudioType(context)
    }

    @Test
    fun checksBluetoothAudioTypeThrowsExceptionWhenChainIsNull() {
        thrown.expect(NavigationException::class.java)
        thrown.expectMessage("Invalid chain for AudioType: next element for Bluetooth didn't set")
        val bluetoothAudioType = AudioTypeResolver.Bluetooth()
        val audioManager = mockk<AudioManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every {
                getSystemService(Context.AUDIO_SERVICE)
            } returns audioManager
        }

        bluetoothAudioType.obtainAudioType(context)
    }
}
