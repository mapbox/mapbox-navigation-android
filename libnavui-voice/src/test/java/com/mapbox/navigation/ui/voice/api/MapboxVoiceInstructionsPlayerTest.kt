package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.AudioManager
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.api.VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer
import com.mapbox.navigation.ui.voice.api.VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer
import com.mapbox.navigation.ui.voice.model.AudioFocusOwner
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
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class MapboxVoiceInstructionsPlayerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val aMockedContext: Context = mockk(relaxed = true)
    private val audioManager = mockk<AudioManager>(relaxed = true)
    private val mockedAudioFocusDelegate: AsyncAudioFocusDelegate = mockk(relaxed = true)
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
            AudioFocusDelegateProvider.defaultAudioFocusDelegate(
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
    fun updateLanguage() {
        val newLanguage = "IT"
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk(relaxed = true)
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk(relaxed = true)
        every {
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerOptions
            )

        mapboxVoiceInstructionsPlayer.updateLanguage(newLanguage)

        verify(exactly = 1) { mockedTextPlayer.updateLanguage(newLanguage) }
    }

    @Test
    fun `play VoiceInstructionsFilePlayer if file available`() {
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
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        val requestSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.requestFocus(
                AudioFocusOwner.MediaPlayer,
                capture(requestSlotCallback),
            )
        } answers {
            requestSlotCallback.captured.invoke(true)
        }

        val abandonFocusSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.abandonFocus(capture(abandonFocusSlotCallback))
        } answers {
            abandonFocusSlotCallback.captured.invoke(true)
        }

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
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        val requestSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.requestFocus(
                AudioFocusOwner.MediaPlayer,
                capture(requestSlotCallback)
            )
        } answers {
            requestSlotCallback.captured.invoke(false)
        }

        val abandonFocusSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.abandonFocus(capture(abandonFocusSlotCallback))
        } answers {
            abandonFocusSlotCallback.captured.invoke(true)
        }

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
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        val requestSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.requestFocus(
                AudioFocusOwner.TextToSpeech,
                capture(requestSlotCallback),
            )
        } answers {
            requestSlotCallback.captured.invoke(true)
        }

        val abandonFocusSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.abandonFocus(capture(abandonFocusSlotCallback))
        } answers {
            abandonFocusSlotCallback.captured.invoke(true)
        }

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
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer

        val requestSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.requestFocus(any(), capture(requestSlotCallback))
        } answers {
            requestSlotCallback.captured.invoke(true)
        }

        val abandonFocusSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.abandonFocus(capture(abandonFocusSlotCallback))
        } answers {
            abandonFocusSlotCallback.captured.invoke(true)
        }

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
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
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        every { mockedFilePlayer.volume(any()) } just Runs
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        every { mockedTextPlayer.volume(any()) } just Runs
        every {
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
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
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        every { mockedFilePlayer.volume(any()) } just Runs
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        every { mockedTextPlayer.volume(any()) } just Runs
        every {
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedVolume: SpeechVolume = mockk()
        every { mockedVolume.level } returns -0.5f

        mapboxVoiceInstructionsPlayer.volume(mockedVolume)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid volume greater than max (1)`() {
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        every { mockedFilePlayer.volume(any()) } just Runs
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        every { mockedTextPlayer.volume(any()) } just Runs
        every {
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedVolume: SpeechVolume = mockk()
        every { mockedVolume.level } returns 1.5f

        mapboxVoiceInstructionsPlayer.volume(mockedVolume)
    }

    @Test
    fun clear() {
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        every { mockedFilePlayer.clear() } just Runs
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        every { mockedTextPlayer.clear() } just Runs
        every {
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
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
        verify(exactly = 1) {
            mockedAudioFocusDelegate.abandonFocus(any())
        }
    }

    @Test
    fun shutdown() {
        val anyLanguage = Locale.US.language
        val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk()
        every { mockedFilePlayer.shutdown() } just Runs
        val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk()
        every { mockedTextPlayer.shutdown() } just Runs
        every {
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerAttributes,
            )
        } returns mockedTextPlayer
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
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
        verify(exactly = 1) {
            mockedAudioFocusDelegate.abandonFocus(any())
        }
    }

    @Test
    fun `request audio focus when play and abandon focus when done`() {
        val anyLanguage = Locale.US.language
        val mockedAnnouncement: SpeechAnnouncement = mockk(relaxed = true)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer: MapboxNavigationConsumer<SpeechAnnouncement> = mockk()
        every { voiceInstructionsPlayerConsumer.accept(any()) } just Runs

        val requestSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.requestFocus(any(), capture(requestSlotCallback))
        } answers {
            requestSlotCallback.captured.invoke(true)
        }

        val abandonFocusSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.abandonFocus(capture(abandonFocusSlotCallback))
        } answers {
            abandonFocusSlotCallback.captured.invoke(true)
        }

        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerConsumer)

        verify(exactly = 1) {
            mockedAudioFocusDelegate.requestFocus(any(), any())
        }
        verify(exactly = 1) {
            mockedAudioFocusDelegate.abandonFocus(any())
        }
        verifyOrder {
            mockedAudioFocusDelegate.requestFocus(any(), any())
            mockedAudioFocusDelegate.abandonFocus(any())
        }
    }

    @Test
    fun `should abandon focus after a options#abandonFocusDelay`() {
        val announcement: SpeechAnnouncement = SpeechAnnouncement
            .Builder("In 100 meters, turn left.").build()
        val timer = TestTimer()
        val options = VoiceInstructionsPlayerOptions.Builder()
            .abandonFocusDelay(2000L)
            .build()
        given(
            audioFocusResult = true,
            filePlayerCallbackAnswer = { it.onDone(announcement) },
            textPlayerCallbackAnswer = { it.onDone(announcement) }
        )

        val sut = MapboxVoiceInstructionsPlayer(
            context = aMockedContext,
            language = Locale.US.language,
            options = options,
            audioFocusDelegate = mockedAudioFocusDelegate,
            timerFactory = { timer }
        )
        sut.play(announcement) { /* no-op */ }

        assert(timer.tasks.size == 1)
        assert(timer.tasks[0].second == options.abandonFocusDelay)
        verify {
            mockedAudioFocusDelegate.abandonFocus(any())
        }
    }

    @Test
    fun `play is done after clear`() {
        val anyLanguage = Locale.US.language
        val mockedAnnouncement: SpeechAnnouncement = mockk(relaxed = true)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                anyLanguage,
                mockedPlayerOptions
            )
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement
        val voiceInstructionsPlayerConsumer =
            mockk<MapboxNavigationConsumer<SpeechAnnouncement>>(relaxed = true)

        val requestSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.requestFocus(any(), capture(requestSlotCallback))
        } just Runs
        mapboxVoiceInstructionsPlayer.play(mockedPlay, voiceInstructionsPlayerConsumer)
        mapboxVoiceInstructionsPlayer.clear()

        requestSlotCallback.captured.invoke(false)

        verify(exactly = 0) { voiceInstructionsPlayerConsumer.accept(any()) }
    }

    private fun given(
        audioFocusResult: Boolean,
        filePlayerCallbackAnswer: (VoiceInstructionsPlayerCallback) -> Unit,
        textPlayerCallbackAnswer: (VoiceInstructionsPlayerCallback) -> Unit
    ) {
        val audioFocusCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.requestFocus(any(), capture(audioFocusCallback))
        } answers {
            audioFocusCallback.captured.invoke(audioFocusResult)
        }

        val filePlayerCallback = slot<VoiceInstructionsPlayerCallback>()
        val textPlayerCallback = slot<VoiceInstructionsPlayerCallback>()
        every { retrieveVoiceInstructionsFilePlayer(any(), any()) } returns mockk {
            every { play(any(), capture(filePlayerCallback)) } answers {
                filePlayerCallbackAnswer(filePlayerCallback.captured)
            }
        }
        every { retrieveVoiceInstructionsTextPlayer(any(), any(), any()) } returns mockk {
            every { play(any(), capture(textPlayerCallback)) } answers {
                textPlayerCallbackAnswer(textPlayerCallback.captured)
            }
        }
    }
}

private class TestTimer : Timer() {
    val tasks = mutableListOf<Pair<TimerTask, Long>>()
    override fun schedule(task: TimerTask, delay: Long) {
        tasks.add(task to delay)
        task.run()
    }
}
