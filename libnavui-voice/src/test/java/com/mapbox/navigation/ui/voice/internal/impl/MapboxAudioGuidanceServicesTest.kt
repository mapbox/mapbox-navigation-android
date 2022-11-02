package com.mapbox.navigation.ui.voice.internal.impl

import android.content.Context
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.voice.api.AudioFocusDelegateProvider
import com.mapbox.navigation.ui.voice.api.MapboxVoiceApi
import com.mapbox.navigation.ui.voice.api.VoiceApiProvider
import com.mapbox.navigation.ui.voice.api.VoiceInstructionsTextPlayer
import com.mapbox.navigation.ui.voice.api.VoiceInstructionsTextPlayerProvider
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
            newLanguage
        )

        assertEquals(firstInstance, secondInstance)
        verify { textPlayer.updateLanguage(newLanguage) }
    }

    @Test
    fun `get mapboxAudioGuidanceVoice twice`() {
        val context = mockk<Context>(relaxed = true)
        val token = "pk"
        val voiceAPIEn = mockk<MapboxVoiceApi>(relaxed = true)
        val voiceAPIIt = mockk<MapboxVoiceApi>(relaxed = true)
        every { mapboxNavigation.navigationOptions } returns mockk(relaxed = true) {
            every { accessToken } returns token
            every { applicationContext } returns context
        }
        mockkObject(VoiceApiProvider) {
            every {
                VoiceApiProvider.retrieveMapboxVoiceApi(any(), any(), "en", any())
            } returns voiceAPIEn
            every {
                VoiceApiProvider.retrieveMapboxVoiceApi(any(), any(), "it", any())
            } returns voiceAPIIt
            sut.mapboxAudioGuidanceVoice(mapboxNavigation, "en")

            verify(exactly = 0) { voiceAPIEn.destroy() }

            sut.mapboxAudioGuidanceVoice(mapboxNavigation, "it")

            verify(exactly = 1) { voiceAPIEn.destroy() }
            verify(exactly = 0) { voiceAPIIt.destroy() }
        }
    }
}
