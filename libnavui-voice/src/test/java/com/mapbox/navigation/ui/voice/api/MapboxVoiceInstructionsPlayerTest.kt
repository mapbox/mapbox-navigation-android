package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.AudioManager
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
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

    private val aMockedContext: Context = mockk(relaxed = true)
    private val audioManager = mockk<AudioManager>(relaxed = true)
    private val mockedAudioFocusDelegate: AudioFocusDelegate = mockk(relaxed = true)
    private val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk(relaxed = true)
    private val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(VoiceInstructionsFilePlayerProvider)
        mockkObject(VoiceInstructionsTextPlayerProvider)
        mockkObject(AudioFocusDelegateProvider)
        mockkObject(VoiceInstructionsPlayerAttributesProvider)
        every {
            aMockedContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        } returns audioManager
        every { mockedPlayerAttributes.options } returns mockedPlayerOptions
        every {
            AudioFocusDelegateProvider.retrieveAudioFocusDelegate(
                audioManager,
                mockedPlayerAttributes
            )
        } returns mockedAudioFocusDelegate
        every {
            VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(
                mockedPlayerOptions
            )
        } returns mockedPlayerAttributes
    }

    @After
    fun tearDown() {
        unmockkObject(VoiceInstructionsFilePlayerProvider)
        unmockkObject(VoiceInstructionsTextPlayerProvider)
        unmockkObject(AudioFocusDelegateProvider)
        unmockkObject(VoiceInstructionsPlayerAttributesProvider)
    }

    @Test
    fun `play VoiceInstructionsFilePlayer if file available`() {
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        val voiceInstructionsPlayerCallbackSlot = slot<VoiceInstructionsPlayerCallback>()
        val mockedAnnouncement: SpeechAnnouncement = mockk()
        val mockedFile: File = mockk()
        every { mockedAnnouncement.file } returns mockedFile
        val mockedDonePlaying: SpeechAnnouncement = mockedAnnouncement
        every {
            mockedFilePlayer.play(any(), capture(voiceInstructionsPlayerCallbackSlot))
        } answers {
            voiceInstructionsPlayerCallbackSlot.captured.onDone(mockedDonePlaying)
        }
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer

        every {
            mockedAudioFocusDelegate.requestFocus()
        } returns true

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerConsumer)

        verify(exactly = 1) {
            mockedFilePlayer.play(mockedPlay, any())
        }
        verify(exactly = 0) {
            mockedTextPlayer.play(mockedPlay, any())
        }
        verify(exactly = 1) {
            voiceInstructionsPlayerConsumer.accept(mockedPlay)
        }
    }

    @Test
    fun `don't play VoiceInstruction if focus not granted`() {
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        val voiceInstructionsPlayerCallbackSlot = slot<VoiceInstructionsPlayerCallback>()
        val mockedAnnouncement: SpeechAnnouncement = mockk()
        val mockedFile: File = mockk()
        every { mockedAnnouncement.file } returns mockedFile
        val mockedDonePlaying: SpeechAnnouncement = mockedAnnouncement
        every {
            mockedFilePlayer.play(any(), capture(voiceInstructionsPlayerCallbackSlot))
        } answers {
            voiceInstructionsPlayerCallbackSlot.captured.onDone(mockedDonePlaying)
        }
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer

        every {
            mockedAudioFocusDelegate.requestFocus()
        } returns false

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerConsumer)

        verify(exactly = 0) {
            mockedFilePlayer.play(mockedPlay, any())
        }
        verify(exactly = 0) {
            mockedTextPlayer.play(mockedPlay, any())
        }
        verify(exactly = 1) {
            voiceInstructionsPlayerConsumer.accept(mockedPlay)
        }
    }

    @Test
    fun `play VoiceInstructionsTextPlayer if file not available`() {
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        val voiceInstructionsPlayerCallbackSlot = slot<VoiceInstructionsPlayerCallback>()
        val mockedAnnouncement: SpeechAnnouncement = mockk()
        val nullFile = null
        every { mockedAnnouncement.file } returns nullFile
        val mockedDonePlaying: SpeechAnnouncement = mockedAnnouncement
        every {
            mockedTextPlayer.play(any(), capture(voiceInstructionsPlayerCallbackSlot))
        } answers {
            voiceInstructionsPlayerCallbackSlot.captured.onDone(mockedDonePlaying)
        }
        every {
            VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                anyAccessToken,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer

        every {
            mockedAudioFocusDelegate.requestFocus()
        } returns true

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerConsumer)

        verify(exactly = 1) {
            mockedTextPlayer.play(mockedPlay, any())
        }
        verify(exactly = 0) {
            mockedFilePlayer.play(mockedPlay, any())
        }
        verify(exactly = 1) {
            voiceInstructionsPlayerConsumer.accept(mockedPlay)
        }
    }

    @Test
    fun `announcements are played in order`() {
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        val voiceInstructionsPlayerCallbackSlot = slot<VoiceInstructionsPlayerCallback>()
        val mockedAnnouncement: SpeechAnnouncement = mockk()
        val mockedFile: File = mockk()
        val nullFile = null
        every { mockedAnnouncement.file } returns mockedFile andThen nullFile
        val mockedDonePlaying: SpeechAnnouncement = mockedAnnouncement
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
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer

        every {
            mockedAudioFocusDelegate.requestFocus()
        } returns true

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerConsumer)
        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerConsumer)

        verifyOrder {
            mockedFilePlayer.play(mockedPlay, any())
            voiceInstructionsPlayerConsumer.accept(mockedPlay)
            mockedTextPlayer.play(mockedPlay, any())
            voiceInstructionsPlayerConsumer.accept(mockedPlay)
        }
    }

    @Test
    fun volume() {
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
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedVolume: SpeechVolume = mockk()
        every { mockedVolume.level } returns 0.5f

        mapboxVoiceInstructionsPlayer.volume(mockedVolume)

        verify(exactly = 1) {
            mockedFilePlayer.volume(mockedVolume)
        }
        verify(exactly = 1) {
            mockedTextPlayer.volume(mockedVolume)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid volume less than min (0)`() {
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
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedVolume: SpeechVolume = mockk()
        every { mockedVolume.level } returns -0.5f

        mapboxVoiceInstructionsPlayer.volume(mockedVolume)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid volume greater than max (1)`() {
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
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedVolume: SpeechVolume = mockk()
        every { mockedVolume.level } returns 1.5f

        mapboxVoiceInstructionsPlayer.volume(mockedVolume)
    }

    @Test
    fun clear() {
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
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )

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
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )

        mapboxVoiceInstructionsPlayer.shutdown()

        verify(exactly = 1) {
            mockedFilePlayer.shutdown()
        }
        verify(exactly = 1) {
            mockedTextPlayer.shutdown()
        }
    }

    @Test
    fun `request audio focus when play and abandon focus when done`() {
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedAnnouncement: SpeechAnnouncement = mockk(relaxed = true)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerConsumer)

        verify(exactly = 1) {
            mockedAudioFocusDelegate.requestFocus()
        }
        verify(exactly = 1) {
            mockedAudioFocusDelegate.abandonFocus()
        }
        verifyOrder {
            mockedAudioFocusDelegate.requestFocus()
            mockedAudioFocusDelegate.abandonFocus()
        }
    }
}
