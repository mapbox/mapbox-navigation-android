package com.mapbox.navigation.voice.impl

import android.content.Context
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.voice.api.AudioFocusDelegateProvider
import com.mapbox.navigation.voice.api.VoiceInstructionsTextPlayer
import com.mapbox.navigation.voice.api.VoiceInstructionsTextPlayerProvider
import com.mapbox.navigation.voice.internal.impl.MapboxAudioGuidanceServices
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

class MapboxAudioGuidanceServicesTest {

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val textPlayer = mockk<VoiceInstructionsTextPlayer>(relaxed = true)
    private val sut = MapboxAudioGuidanceServices()

    @Before
    fun setUp() {
        mockkObject(AudioFocusDelegateProvider)
        every {
            AudioFocusDelegateProvider.defaultAudioFocusDelegate(any<Context>(), any())
        } returns mockk(relaxed = true)
        mockkObject(VoiceInstructionsTextPlayerProvider)
        every {
            VoiceInstructionsTextPlayerProvider
                .retrieveVoiceInstructionsTextPlayer(any(), any(), any())
        } returns textPlayer
    }

    @After
    fun tearDown() {
        unmockkObject(AudioFocusDelegateProvider)
        unmockkObject(VoiceInstructionsTextPlayerProvider)
    }

    @Test
    fun getOrUpdateMapboxVoiceInstructionsPlayer() {
        val newLanguage = "it"
        val firstInstance = sut.getOrUpdateMapboxVoiceInstructionsPlayer(mapboxNavigation, "en")

        val secondInstance = sut.getOrUpdateMapboxVoiceInstructionsPlayer(
            mapboxNavigation,
            newLanguage,
        )

        assertEquals(firstInstance, secondInstance)
        verify { textPlayer.updateLanguage(newLanguage) }
    }
}
