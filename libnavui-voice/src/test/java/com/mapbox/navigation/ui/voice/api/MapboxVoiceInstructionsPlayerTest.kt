package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.navigation.ui.base.api.voice.VoiceInstructionsPlayerCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Locale

class MapboxVoiceInstructionsPlayerTest {

    @Before
    fun setUp() {
        mockkObject(VoiceInstructionsFilePlayerProvider)
        mockkObject(VoiceInstructionsTextPlayerProvider)
    }

    @After
    fun tearDown() {
        unmockkObject(VoiceInstructionsFilePlayerProvider)
        unmockkObject(VoiceInstructionsTextPlayerProvider)
    }

    @Test
    fun `play VoiceInstructionsFilePlayer if file available`() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        val voiceInstructionsPlayerCallbackSlot = slot<VoiceInstructionsPlayerCallback>()
        val mockedAnnouncement: Announcement = mockk()
        val mockedFile: File = mockk()
        every { mockedAnnouncement.file } returns mockedFile
        val mockedDonePlaying: SpeechState.DonePlaying = SpeechState.DonePlaying(mockedAnnouncement)
        every {
            mockedFilePlayer.play(any(), capture(voiceInstructionsPlayerCallbackSlot))
        } answers {
            voiceInstructionsPlayerCallbackSlot.captured.onDone(mockedDonePlaying)
        }
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(aMockedContext, anyAccessToken, anyLanguage)
        val mockedPlay: SpeechState.ReadyToPlay = SpeechState.ReadyToPlay(mockedAnnouncement)
        val voiceInstructionsPlayerCallback: VoiceInstructionsPlayerCallback = mockk()
        every { voiceInstructionsPlayerCallback.onDone(any()) } just Runs

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerCallback)

        verify(exactly = 1) {
            mockedFilePlayer.play(mockedPlay, any())
        }
        verify(exactly = 0) {
            mockedTextPlayer.play(mockedPlay, any())
        }
        verify(exactly = 1) {
            voiceInstructionsPlayerCallback.onDone(SpeechState.DonePlaying(mockedPlay.announcement))
        }
    }

    @Test
    fun `play VoiceInstructionsTextPlayer if file not available`() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        val voiceInstructionsPlayerCallbackSlot = slot<VoiceInstructionsPlayerCallback>()
        val mockedAnnouncement: Announcement = mockk()
        val nullFile = null
        every { mockedAnnouncement.file } returns nullFile
        val mockedDonePlaying: SpeechState.DonePlaying = SpeechState.DonePlaying(mockedAnnouncement)
        every {
            mockedTextPlayer.play(any(), capture(voiceInstructionsPlayerCallbackSlot))
        } answers {
            voiceInstructionsPlayerCallbackSlot.captured.onDone(mockedDonePlaying)
        }
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(aMockedContext, anyAccessToken, anyLanguage)
        val mockedPlay: SpeechState.ReadyToPlay = SpeechState.ReadyToPlay(mockedAnnouncement)
        val voiceInstructionsPlayerCallback: VoiceInstructionsPlayerCallback = mockk()
        every { voiceInstructionsPlayerCallback.onDone(any()) } just Runs

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerCallback)

        verify(exactly = 1) {
            mockedTextPlayer.play(mockedPlay, any())
        }
        verify(exactly = 0) {
            mockedFilePlayer.play(mockedPlay, any())
        }
        verify(exactly = 1) {
            voiceInstructionsPlayerCallback.onDone(SpeechState.DonePlaying(mockedPlay.announcement))
        }
    }

    @Test
    fun `announcements are played in order`() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        val voiceInstructionsPlayerCallbackSlot = slot<VoiceInstructionsPlayerCallback>()
        val mockedAnnouncement: Announcement = mockk()
        val mockedFile: File = mockk()
        val nullFile = null
        every { mockedAnnouncement.file } returns mockedFile andThen nullFile
        val mockedDonePlaying: SpeechState.DonePlaying = SpeechState.DonePlaying(mockedAnnouncement)
        every {
            mockedFilePlayer.play(any(), capture(voiceInstructionsPlayerCallbackSlot))
        } answers {
            voiceInstructionsPlayerCallbackSlot.captured.onDone(mockedDonePlaying)
        }
        every {
            mockedTextPlayer.play(any(), capture(voiceInstructionsPlayerCallbackSlot))
        } answers {
            voiceInstructionsPlayerCallbackSlot.captured.onDone(mockedDonePlaying)
        }
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(aMockedContext, anyAccessToken, anyLanguage)
        val mockedPlay: SpeechState.ReadyToPlay = SpeechState.ReadyToPlay(mockedAnnouncement)
        val voiceInstructionsPlayerCallback: VoiceInstructionsPlayerCallback = mockk()
        every { voiceInstructionsPlayerCallback.onDone(any()) } just Runs

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerCallback)
        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerCallback)

        verifyOrder {
            mockedFilePlayer.play(mockedPlay, any())
            voiceInstructionsPlayerCallback.onDone(SpeechState.DonePlaying(mockedPlay.announcement))
            mockedTextPlayer.play(mockedPlay, any())
            voiceInstructionsPlayerCallback.onDone(SpeechState.DonePlaying(mockedPlay.announcement))
        }
    }

    @Test
    fun volume() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        every { mockedFilePlayer.volume(any()) } just Runs
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        every { mockedTextPlayer.volume(any()) } just Runs
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(aMockedContext, anyAccessToken, anyLanguage)
        val mockedVolume: SpeechState.Volume = mockk()

        mapboxVoiceInstructionsPlayer.volume(mockedVolume)

        verify(exactly = 1) {
            mockedFilePlayer.volume(mockedVolume)
        }
        verify(exactly = 1) {
            mockedTextPlayer.volume(mockedVolume)
        }
    }

    @Test
    fun clear() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        every { mockedFilePlayer.clear() } just Runs
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        every { mockedTextPlayer.clear() } just Runs
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(aMockedContext, anyAccessToken, anyLanguage)

        mapboxVoiceInstructionsPlayer.clear()

        verify(exactly = 1) {
            mockedFilePlayer.clear()
        }
        verify(exactly = 1) {
            mockedTextPlayer.clear()
        }
    }

    @Test
    fun shutdown() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        every { mockedFilePlayer.shutdown() } just Runs
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        every { mockedTextPlayer.shutdown() } just Runs
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(aMockedContext, anyAccessToken, anyLanguage)

        mapboxVoiceInstructionsPlayer.shutdown()

        verify(exactly = 1) {
            mockedFilePlayer.shutdown()
        }
        verify(exactly = 1) {
            mockedTextPlayer.shutdown()
        }
    }
}
