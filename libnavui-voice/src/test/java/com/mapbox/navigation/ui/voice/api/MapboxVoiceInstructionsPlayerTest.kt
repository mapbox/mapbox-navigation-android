package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.AudioManager
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.api.VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer
import com.mapbox.navigation.ui.voice.api.VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer
import com.mapbox.navigation.ui.voice.model.AudioFocusOwner
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxVoiceInstructionsPlayerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val aMockedContext: Context = mockk(relaxed = true)
    private val audioManager = mockk<AudioManager>(relaxed = true)
    private val mockedAudioFocusDelegate: AsyncAudioFocusDelegate = mockk(relaxed = true)
    private val mockedPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk(relaxed = true)
    private val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk(relaxed = true)
    private val mockedFilePlayer: VoiceInstructionsFilePlayer = mockk(relaxed = true)
    private val mockedTextPlayer: VoiceInstructionsTextPlayer = mockk(relaxed = true)
    private val voiceInstructionsPlayerConsumer =
        mockk<MapboxNavigationConsumer<SpeechAnnouncement>>(relaxed = true)
    private val language = Locale.US.language

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
        every {
            retrieveVoiceInstructionsFilePlayer(
                aMockedContext,
                mockedPlayerAttributes,
            )
        } returns mockedFilePlayer
        every {
            retrieveVoiceInstructionsTextPlayer(
                aMockedContext,
                language,
                mockedPlayerAttributes,
                any(),
            )
        } returns mockedTextPlayer
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
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()

        mapboxVoiceInstructionsPlayer.updateLanguage(newLanguage)

        verify(exactly = 1) { mockedTextPlayer.updateLanguage(newLanguage) }
    }

    @Test
    fun `play VoiceInstructionsFilePlayer if file available`() {
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

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement

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
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement

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

        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement

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
                language,
                mockedPlayerOptions
            )
        finishInitialization()
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement

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
        every { mockedFilePlayer.volume(any()) } just Runs
        every { mockedTextPlayer.volume(any()) } just Runs
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()
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
        every { mockedFilePlayer.volume(any()) } just Runs
        every { mockedTextPlayer.volume(any()) } just Runs
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()
        val mockedVolume: SpeechVolume = mockk()
        every { mockedVolume.level } returns -0.5f

        mapboxVoiceInstructionsPlayer.volume(mockedVolume)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid volume greater than max (1)`() {
        every { mockedFilePlayer.volume(any()) } just Runs
        every { mockedTextPlayer.volume(any()) } just Runs
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()
        val mockedVolume: SpeechVolume = mockk()
        every { mockedVolume.level } returns 1.5f

        mapboxVoiceInstructionsPlayer.volume(mockedVolume)
    }

    @Test
    fun clear() {
        every { mockedFilePlayer.clear() } just Runs
        every { mockedTextPlayer.clear() } just Runs
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()

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
        every { mockedFilePlayer.shutdown() } just Runs
        every { mockedTextPlayer.shutdown() } just Runs
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()

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
        every { mockedFilePlayer.play(any(), any()) } answers {
            (secondArg() as VoiceInstructionsPlayerCallback).onDone(firstArg())
        }
        val mockedAnnouncement: SpeechAnnouncement = mockk(relaxed = true)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        finishInitialization()
        val mockedPlay: SpeechAnnouncement = mockedAnnouncement

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
        every { mockedFilePlayer.play(any(), any()) } answers {
            (secondArg() as VoiceInstructionsPlayerCallback).onDone(firstArg())
        }
        val announcement: SpeechAnnouncement = SpeechAnnouncement
            .Builder("In 100 meters, turn left.").build()
        val timer = spyk(instantTimer())
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
        finishInitialization()
        sut.play(announcement) { /* no-op */ }

        verifyOrder {
            timer.schedule(any(), options.abandonFocusDelay)
            mockedAudioFocusDelegate.abandonFocus(any())
        }
    }

    @Test
    fun `play text before initialization with default timeout plays`() =
        coroutineRule.runBlockingTest {
            val announcement = textAnnouncement()
            setUpPlayer(mockedTextPlayer)
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )
            setUpAudioFocus()

            mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer)

            `check that instruction is in the queue`()

            coroutineRule.testDispatcher.advanceTimeBy(499)
            finishInitialization()

            `check that text instruction is played`(announcement)
        }

    @Test
    fun `play text before initialization with default timeout cancels`() =
        coroutineRule.runBlockingTest {
            val announcement = textAnnouncement()
            setUpPlayer(mockedTextPlayer)
            setUpAudioFocus()
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )

            mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer)
            coroutineRule.testDispatcher.advanceTimeBy(499)

            `check that instruction is in the queue`()

            coroutineRule.testDispatcher.advanceTimeBy(2)
            finishInitialization()

            `check that instruction is cancelled`(announcement)
        }

    @Test
    fun `play text before initialization with custom timeout plays`() =
        coroutineRule.runBlockingTest {
            val announcement = textAnnouncement()
            setUpPlayer(mockedTextPlayer)
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )
            setUpAudioFocus()

            mapboxVoiceInstructionsPlayer.play(
                announcement,
                voiceInstructionsPlayerConsumer,
                1000
            )
            coroutineRule.testDispatcher.advanceTimeBy(501)

            `check that instruction is in the queue`()

            coroutineRule.testDispatcher.advanceTimeBy(498)
            finishInitialization()

            `check that text instruction is played`(announcement)
        }

    @Test
    fun `play text before initialization with custom timeout cancels`() =
        coroutineRule.runBlockingTest {
            val announcement = textAnnouncement()
            setUpPlayer(mockedTextPlayer)
            setUpAudioFocus()
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )

            mapboxVoiceInstructionsPlayer.play(
                announcement,
                voiceInstructionsPlayerConsumer,
                1000
            )
            coroutineRule.testDispatcher.advanceTimeBy(999)

            `check that instruction is in the queue`()

            coroutineRule.testDispatcher.advanceTimeBy(2)
            finishInitialization()

            `check that instruction is cancelled`(announcement)
        }

    @Test
    fun `play text before initialization with null timeout plays`() =
        coroutineRule.runBlockingTest {
            val announcement = textAnnouncement()
            setUpPlayer(mockedTextPlayer)
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )
            setUpAudioFocus()

            mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer, null)
            coroutineRule.testDispatcher.advanceTimeBy(1000)

            `check that instruction is in the queue`()

            coroutineRule.testDispatcher.advanceTimeBy(1000)
            finishInitialization()

            `check that text instruction is played`(announcement)
        }

    @Test
    fun `play file before initialization with default timeout plays`() =
        coroutineRule.runBlockingTest {
            val announcement = fileAnnouncement()
            setUpPlayer(mockedFilePlayer)
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )
            setUpAudioFocus()

            mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer)

            `check that instruction is in the queue`()

            coroutineRule.testDispatcher.advanceTimeBy(499)
            finishInitialization()

            `check that file instruction is played`(announcement)
        }

    @Test
    fun `play file before initialization with default timeout cancels`() =
        coroutineRule.runBlockingTest {
            val announcement = fileAnnouncement()
            setUpPlayer(mockedFilePlayer)
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )
            setUpAudioFocus()

            mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer)
            coroutineRule.testDispatcher.advanceTimeBy(499)

            `check that instruction is in the queue`()

            coroutineRule.testDispatcher.advanceTimeBy(2)
            finishInitialization()

            `check that instruction is cancelled`(announcement)
        }

    @Test
    fun `play file before initialization with custom timeout plays`() =
        coroutineRule.runBlockingTest {
            val announcement = fileAnnouncement()
            setUpPlayer(mockedFilePlayer)
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )
            setUpAudioFocus()

            mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer, 1000)

            `check that instruction is in the queue`()

            coroutineRule.testDispatcher.advanceTimeBy(999)
            finishInitialization()

            `check that file instruction is played`(announcement)
        }

    @Test
    fun `play file before initialization with custom timeout cancels`() {
        val announcement = fileAnnouncement()
        setUpPlayer(mockedFilePlayer)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        setUpAudioFocus()

        mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer, 1000)
        coroutineRule.testDispatcher.advanceTimeBy(999)

        `check that instruction is in the queue`()

        coroutineRule.testDispatcher.advanceTimeBy(2)
        finishInitialization()

        `check that instruction is cancelled`(announcement)
    }

    @Test
    fun `play file before initialization with null timeout plays`() {
        val announcement = fileAnnouncement()
        setUpPlayer(mockedFilePlayer)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        setUpAudioFocus()

        mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer, null)
        coroutineRule.testDispatcher.advanceTimeBy(1000)

        `check that instruction is in the queue`()

        coroutineRule.testDispatcher.advanceTimeBy(1000)
        finishInitialization()

        `check that file instruction is played`(announcement)
    }

    @Test
    fun `initialization triggers playing the queue`() = coroutineRule.runBlockingTest {
        val fileAnnouncement = fileAnnouncement()
        val textAnnouncement = textAnnouncement()
        setUpPlayer(mockedFilePlayer)
        setUpPlayer(mockedTextPlayer)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        setUpAudioFocus()

        mapboxVoiceInstructionsPlayer.play(textAnnouncement, voiceInstructionsPlayerConsumer, null)
        mapboxVoiceInstructionsPlayer.play(fileAnnouncement, voiceInstructionsPlayerConsumer, null)

        `check that instruction is in the queue`()

        finishInitialization()

        verifyOrder {
            mockedTextPlayer.play(textAnnouncement, any())
            voiceInstructionsPlayerConsumer.accept(textAnnouncement)
            mockedFilePlayer.play(fileAnnouncement, any())
            voiceInstructionsPlayerConsumer.accept(fileAnnouncement)
        }
        verify(exactly = 0) {
            mockedFilePlayer.cancel(any())
            mockedTextPlayer.cancel(any())
        }
    }

    @Test
    fun `initialization with empty queue`() = coroutineRule.runBlockingTest {
        val textAnnouncement = textAnnouncement()
        val fileAnnouncement = fileAnnouncement()
        setUpPlayer(mockedTextPlayer)
        setUpPlayer(mockedFilePlayer)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        setUpAudioFocus()

        finishInitialization()
        verify(exactly = 0) {
            mockedFilePlayer.play(any(), any())
            mockedTextPlayer.play(any(), any())
        }

        mapboxVoiceInstructionsPlayer.play(textAnnouncement, voiceInstructionsPlayerConsumer)
        mapboxVoiceInstructionsPlayer.play(fileAnnouncement, voiceInstructionsPlayerConsumer)
        verifyOrder {
            mockedTextPlayer.play(textAnnouncement, any())
            voiceInstructionsPlayerConsumer.accept(textAnnouncement)
            mockedFilePlayer.play(fileAnnouncement, any())
            voiceInstructionsPlayerConsumer.accept(fileAnnouncement)
        }
        verify(exactly = 0) {
            mockedFilePlayer.cancel(any())
            mockedTextPlayer.cancel(any())
        }
    }

    @Test
    fun `cancel with empty queue`() = coroutineRule.runBlockingTest {
        val announcement = textAnnouncement()
        setUpPlayer(mockedTextPlayer)
        setUpPlayer(mockedFilePlayer)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        setUpAudioFocus()
        mapboxVoiceInstructionsPlayer.cancel(announcement)

        verify(exactly = 1) {
            mockedFilePlayer.cancel(announcement)
            mockedTextPlayer.cancel(announcement)
        }

        finishInitialization()
        setUpPlayer(mockedTextPlayer)
        clearMocks(mockedTextPlayer, mockedFilePlayer, answers = false)
        mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer)

        `check that text instruction is played`(announcement)
    }

    @Test
    fun `cancel when the announcement has already been played`() = coroutineRule.runBlockingTest {
        val announcement = textAnnouncement()
        setUpPlayer(mockedTextPlayer)
        setUpPlayer(mockedFilePlayer)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        setUpAudioFocus()
        mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer)
        finishInitialization()
        clearMocks(voiceInstructionsPlayerConsumer, answers = false)
        mapboxVoiceInstructionsPlayer.cancel(announcement)

        verify(exactly = 1) {
            mockedFilePlayer.cancel(announcement)
            mockedTextPlayer.cancel(announcement)
        }
        verify(exactly = 0) {
            voiceInstructionsPlayerConsumer.accept(announcement)
        }
    }

    @Test
    fun `cancel when the announcement has not been played yet`() = coroutineRule.runBlockingTest {
        val announcement = textAnnouncement()
        setUpPlayer(mockedTextPlayer)
        setUpPlayer(mockedFilePlayer)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        setUpAudioFocus()
        mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer)
        mapboxVoiceInstructionsPlayer.cancel(announcement)

        verify(exactly = 1) {
            mockedFilePlayer.cancel(announcement)
            mockedTextPlayer.cancel(announcement)
            voiceInstructionsPlayerConsumer.accept(announcement)
        }

        clearMocks(
            mockedTextPlayer,
            mockedFilePlayer,
            voiceInstructionsPlayerConsumer,
            answers = false
        )
        finishInitialization()

        verify(exactly = 0) {
            mockedTextPlayer.play(any(), any())
            mockedFilePlayer.play(any(), any())
            voiceInstructionsPlayerConsumer.accept(announcement)
        }
    }

    @Test
    fun `cancel when the announcement has already been cancelled`() =
        coroutineRule.runBlockingTest {
            val announcement = textAnnouncement()
            setUpPlayer(mockedTextPlayer)
            setUpPlayer(mockedFilePlayer)
            val mapboxVoiceInstructionsPlayer =
                MapboxVoiceInstructionsPlayer(
                    aMockedContext,
                    language,
                    mockedPlayerOptions
                )
            setUpAudioFocus()
            mapboxVoiceInstructionsPlayer.play(announcement, voiceInstructionsPlayerConsumer, 500)
            coroutineRule.testDispatcher.advanceTimeBy(501)
            clearMocks(mockedTextPlayer, mockedFilePlayer, voiceInstructionsPlayerConsumer)

            mapboxVoiceInstructionsPlayer.cancel(announcement)

            verify(exactly = 1) {
                mockedFilePlayer.cancel(announcement)
                mockedTextPlayer.cancel(announcement)
            }
            verify(exactly = 0) {
                voiceInstructionsPlayerConsumer.accept(announcement)
            }
        }

    @Test
    fun `cancel with multiple instructions in the queue`() = coroutineRule.runBlockingTest {
        val textAnnouncement = textAnnouncement()
        val fileAnnouncement = fileAnnouncement()
        setUpPlayer(mockedTextPlayer)
        setUpPlayer(mockedFilePlayer)
        val mapboxVoiceInstructionsPlayer =
            MapboxVoiceInstructionsPlayer(
                aMockedContext,
                language,
                mockedPlayerOptions
            )
        setUpAudioFocus()
        mapboxVoiceInstructionsPlayer.play(textAnnouncement, voiceInstructionsPlayerConsumer)
        mapboxVoiceInstructionsPlayer.play(fileAnnouncement, voiceInstructionsPlayerConsumer)

        mapboxVoiceInstructionsPlayer.cancel(textAnnouncement)

        verify(exactly = 1) {
            mockedFilePlayer.cancel(textAnnouncement)
            mockedTextPlayer.cancel(textAnnouncement)
            voiceInstructionsPlayerConsumer.accept(textAnnouncement)
        }
        verify(exactly = 0) {
            mockedTextPlayer.cancel(fileAnnouncement)
            mockedFilePlayer.cancel(fileAnnouncement)
            voiceInstructionsPlayerConsumer.accept(fileAnnouncement)
        }

        clearMocks(
            mockedTextPlayer,
            mockedFilePlayer,
            voiceInstructionsPlayerConsumer,
            answers = false
        )
        finishInitialization()

        verify(exactly = 0) {
            mockedTextPlayer.play(any(), any())
            voiceInstructionsPlayerConsumer.accept(textAnnouncement)
        }
        verify(exactly = 1) {
            mockedFilePlayer.play(fileAnnouncement, any())
            voiceInstructionsPlayerConsumer.accept(fileAnnouncement)
        }
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
        every { retrieveVoiceInstructionsTextPlayer(any(), any(), any(), any()) } returns mockk {
            every { play(any(), capture(textPlayerCallback)) } answers {
                textPlayerCallbackAnswer(textPlayerCallback.captured)
            }
        }
    }

    private fun instantTimer(): Timer = object : Timer() {
        override fun schedule(task: TimerTask, delay: Long) = task.run()
    }

    private fun finishInitialization() {
        val listeners = mutableListOf<() -> Unit>()
        verify {
            retrieveVoiceInstructionsTextPlayer(
                any(),
                any(),
                any(),
                capture(listeners)
            )
        }
        listeners.last().invoke()
    }

    private fun `check that instruction is in the queue`() {
        verify(exactly = 0) {
            mockedTextPlayer.play(any(), any())
            mockedFilePlayer.play(any(), any())
            voiceInstructionsPlayerConsumer.accept(any())
        }
    }

    private fun `check that text instruction is played`(
        instruction: SpeechAnnouncement,
    ) {
        verify(exactly = 1) {
            mockedTextPlayer.play(instruction, any())
        }
        verify(exactly = 0) {
            mockedFilePlayer.play(instruction, any())
            mockedFilePlayer.cancel(any())
            mockedTextPlayer.cancel(any())
        }
        verify(exactly = 1) {
            voiceInstructionsPlayerConsumer.accept(instruction)
        }
    }

    private fun `check that file instruction is played`(
        instruction: SpeechAnnouncement,
    ) {
        verify(exactly = 1) {
            mockedFilePlayer.play(instruction, any())
        }
        verify(exactly = 0) {
            mockedTextPlayer.play(instruction, any())
            mockedFilePlayer.cancel(any())
            mockedTextPlayer.cancel(any())
        }
        verify(exactly = 1) {
            voiceInstructionsPlayerConsumer.accept(instruction)
        }
    }

    private fun `check that instruction is cancelled`(
        instruction: SpeechAnnouncement,
    ) {
        verify(exactly = 0) {
            mockedTextPlayer.play(any(), any())
            mockedFilePlayer.play(any(), any())
        }
        verify(exactly = 1) {
            mockedFilePlayer.cancel(instruction)
            mockedTextPlayer.cancel(instruction)
            voiceInstructionsPlayerConsumer.accept(instruction)
        }
    }


    private fun setUpPlayer(player: VoiceInstructionsPlayer) {
        val voiceInstructionsPlayerCallbackSlot = slot<VoiceInstructionsPlayerCallback>()
        every {
            player.play(any(), capture(voiceInstructionsPlayerCallbackSlot))
        } answers {
            voiceInstructionsPlayerCallbackSlot.captured.onDone(firstArg())
        }
    }

    private fun textAnnouncement(): SpeechAnnouncement {
        return mockk(relaxed = true) {
            every { file } returns null
        }
    }

    private fun fileAnnouncement(): SpeechAnnouncement {
        return mockk(relaxed = true) {
            every { file } returns mockk(relaxed = true)
        }
    }

    private fun setUpAudioFocus() {
        val requestSlotCallback = slot<AudioFocusRequestCallback>()
        every {
            mockedAudioFocusDelegate.requestFocus(
                any(),
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
    }
}
