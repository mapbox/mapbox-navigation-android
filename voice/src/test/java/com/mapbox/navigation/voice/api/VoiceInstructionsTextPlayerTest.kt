package com.mapbox.navigation.voice.api

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.LANG_AVAILABLE
import android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
import android.speech.tts.TextToSpeech.OnInitListener
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechVolume
import com.mapbox.navigation.voice.options.VoiceInstructionsPlayerOptions
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class VoiceInstructionsTextPlayerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mockedBundle: Bundle = mockk(relaxUnitFun = true)
    private val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
    private val initListener = slot<OnInitListener>()

    @Before
    fun setUp() {
        mockkObject(BundleProvider)
        every { BundleProvider.retrieveBundle() } returns mockedBundle

        mockkObject(TextToSpeechProvider)
        every {
            TextToSpeechProvider.getTextToSpeech(any(), capture(initListener))
        } returns mockedTextToSpeech
    }

    @After
    fun tearDown() {
        unmockkObject(InternalJobControlFactory)
        unmockkObject(BundleProvider)
        unmockkObject(TextToSpeechProvider)
    }

    @Test(expected = IllegalStateException::class)
    fun `only one announcement can be played at a time`() = runTest {
        val textPlayer = initTextPlayer()
        val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        textPlayer.currentPlay = anyAnnouncement

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)
    }

    @Test
    fun `language is not supported current play is null`() = runTest {
        val textPlayer = initTextPlayer()
        val anyAnnouncement = mockedAnnouncement()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = false

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `language is not supported and checkIsLanguageAvailable set to false current play not null`() =
        runTest {
            val textPlayer = initTextPlayer(
                playerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxed = true) {
                    every { options } returns VoiceInstructionsPlayerOptions.Builder()
                        .checkIsLanguageAvailable(false)
                        .build()
                },
            )

            val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)

            every { anyAnnouncement.announcement } returns """
            Turn right onto Frederick Road, Maryland 3 55.
            """.trimIndent()

            every { anyAnnouncement.ssmlAnnouncement } returns """
            <speak>
                <amazon:effect name="drc">
                    <prosody rate="1.08">Turn right onto Frederick Road, Maryland 3 55.</prosody>
                </amazon:effect>
            </speak>
            """.trimIndent()

            val anyVoiceInstructionsPlayerCallback =
                mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
            textPlayer.updateLanguage(Locale.ENGLISH.toLanguageTag())

            textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

            assertEquals(anyAnnouncement, textPlayer.currentPlay)
        }

    @Test
    fun `language is not supported onDone is called`() = runTest {
        val textPlayer = initTextPlayer()
        val anyAnnouncement = mockedAnnouncement()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = false

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify {
            anyVoiceInstructionsPlayerCallback.onDone(anyAnnouncement)
        }
    }

    @Test
    fun `announcement from state is blank current play is null`() = runTest {
        val textPlayer = initTextPlayer()
        val blankAnnouncement = mockk<SpeechAnnouncement>()
        every { blankAnnouncement.announcement } returns "   "
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = true

        textPlayer.play(blankAnnouncement, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `announcement from state is blank onDone is called`() = runTest {
        val textPlayer = initTextPlayer()
        val blankAnnouncement = mockk<SpeechAnnouncement>()
        every { blankAnnouncement.announcement } returns "   "
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = false

        textPlayer.play(blankAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify {
            anyVoiceInstructionsPlayerCallback.onDone(blankAnnouncement)
        }
    }

    @Test
    fun `putFloat with volume level is called when play`() = runTest {
        val textPlayer = initTextPlayer()
        val anyNonBlankAnnouncement = mockk<SpeechAnnouncement>()
        every { anyNonBlankAnnouncement.announcement } returns "Turn right."
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = true

        textPlayer.play(anyNonBlankAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify { mockedBundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) }
    }

    @Test
    fun `player attributes applyOn with text to speech and bundle is called when play`() =
        runTest {
            val anyPlayerAttributes: VoiceInstructionsPlayerAttributes = mockk(relaxed = true) {
                every { options } returns mockk(relaxed = true) {
                    every { checkIsLanguageAvailable } returns true
                }
            }
            val textPlayer = initTextPlayer(
                playerAttributes = anyPlayerAttributes,
            ).also {
                it.isLanguageSupported = true
            }

            val anyNonBlankAnnouncement = mockk<SpeechAnnouncement>()
            every { anyNonBlankAnnouncement.announcement } returns "Turn right."
            val anyVoiceInstructionsPlayerCallback =
                mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
            textPlayer.play(anyNonBlankAnnouncement, anyVoiceInstructionsPlayerCallback)
            delay(1000)
            verify { anyPlayerAttributes.applyOn(mockedTextToSpeech, mockedBundle) }
        }

    @Test
    fun `text to speech speak with announcement and bundle is called when play`() =
        runTest {
            val textPlayer = initTextPlayer()
            val turnRightAnnouncement = "Turn right."
            val anyNonBlankAnnouncement = mockk<SpeechAnnouncement>()
            every { anyNonBlankAnnouncement.announcement } returns turnRightAnnouncement
            val anyVoiceInstructionsPlayerCallback =
                mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
            textPlayer.isLanguageSupported = true

            textPlayer.play(anyNonBlankAnnouncement, anyVoiceInstructionsPlayerCallback)

            coVerify {
                textPlayer.awaitTextToSpeech()!!.speak(
                    turnRightAnnouncement,
                    TextToSpeech.QUEUE_FLUSH,
                    mockedBundle,
                    "default_id",
                )
            }
        }

    @Test
    fun `volume level is updated when volume`() = runTest {
        val textPlayer = initTextPlayer()
        val aSpeechVolume = SpeechVolume(0.5f)

        textPlayer.volume(aSpeechVolume)

        assertEquals(0.5f, textPlayer.volumeLevel)
    }

    @Test
    fun `if announcing and volume is muted text to speech stop is called when volume`() =
        runTest {
            every { mockedTextToSpeech.isSpeaking } returns true
            val textPlayer = initTextPlayer()
            val mute = SpeechVolume(0.0f)

            textPlayer.volume(mute)

            coVerify { textPlayer.awaitTextToSpeech()!!.stop() }
        }

    @Test
    fun `text to speech stop is called is called when clear`() = runTest {
        val textPlayer = initTextPlayer()

        textPlayer.clear()

        coVerify {
            textPlayer.awaitTextToSpeech()!!.stop()
        }
    }

    @Test
    fun `current play null after clear`() = runTest {
        val textPlayer = initTextPlayer()

        textPlayer.clear()

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `text to speech setOnUtteranceProgressListener with null is called when shutdown`() =
        runTest {
            val textPlayer = initTextPlayer()
            invokeOnInitListener(TextToSpeech.SUCCESS)
            val tts = textPlayer.awaitTextToSpeech()
            textPlayer.shutdown()

            verify {
                tts!!.setOnUtteranceProgressListener(null)
            }
        }

    @Test
    fun `text to speech shutdown is called when shutdown`() = runTest {
        val textPlayer = initTextPlayer()
        val tts = textPlayer.awaitTextToSpeech()
        textPlayer.shutdown()

        verify {
            tts!!.shutdown()
        }
    }

    @Test
    fun `current play is null after shutdown`() = runTest {
        val textPlayer = initTextPlayer()

        textPlayer.shutdown()

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `volume level is reset to default after shutdown`() = runTest {
        val textPlayer = initTextPlayer()
        textPlayer.volumeLevel = 0.5f

        textPlayer.shutdown()

        assertEquals(1.0f, textPlayer.volumeLevel)
    }

    @Test
    fun `updateLanguage initialized with success new language is supported`() =
        runTest {
            val newLanguage = "fr"

            val anyAnnouncement = mockedAnnouncement()
            val anyVoiceInstructionsPlayerCallback =
                mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
            val textPlayer = initTextPlayer()
            every {
                mockedTextToSpeech.isLanguageAvailable(Locale(newLanguage))
            } returns LANG_AVAILABLE
            clearMocks(mockedTextToSpeech, answers = false)

            textPlayer.updateLanguage(newLanguage)

            verify(exactly = 1) {
                mockedTextToSpeech.language = Locale(newLanguage)
            }
            verify(exactly = 0) {
                mockedTextToSpeech.setOnUtteranceProgressListener(any())
            }

            textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

            verify(exactly = 1) { mockedTextToSpeech.speak(any(), any(), any(), any()) }
        }

    @Test
    fun `updateLanguage initialized with success new language is not supported`() =
        runTest {
            val newLanguage = "fr"
            val anyAnnouncement = mockedAnnouncement()
            val anyVoiceInstructionsPlayerCallback =
                mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
            val textPlayer = initTextPlayer()
            every {
                mockedTextToSpeech.isLanguageAvailable(Locale(newLanguage))
            } returns LANG_NOT_SUPPORTED
            invokeOnInitListener(TextToSpeech.SUCCESS)
            clearMocks(mockedTextToSpeech, answers = false)

            textPlayer.updateLanguage(newLanguage)

            verify(exactly = 0) {
                mockedTextToSpeech.language = any()
                mockedTextToSpeech.setOnUtteranceProgressListener(any())
            }

            textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

            verify(exactly = 0) { mockedTextToSpeech.speak(any(), any(), any(), any()) }
            verify(exactly = 1) { anyVoiceInstructionsPlayerCallback.onDone(anyAnnouncement) }
        }

    @Test
    fun `updateLanguage initialized with error`() = runTest {
        val newLanguage = "fr"
        val anyAnnouncement = mockedAnnouncement()
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        val textPlayer = initTextPlayer(
            status = TextToSpeech.ERROR,
        )
        every { mockedTextToSpeech.isLanguageAvailable(Locale(newLanguage)) } returns LANG_AVAILABLE
        clearMocks(mockedTextToSpeech, answers = false)

        textPlayer.updateLanguage(newLanguage)

        verify(exactly = 0) {
            mockedTextToSpeech.language = any()
            mockedTextToSpeech.setOnUtteranceProgressListener(any())
        }

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify(exactly = 0) { mockedTextToSpeech.speak(any(), any(), any(), any()) }
        verify(exactly = 1) { anyVoiceInstructionsPlayerCallback.onDone(anyAnnouncement) }
    }

    @Test
    fun `updateLanguage before initialized with success new language is supported`() = runTest {
        val newLanguage = "fr"

        val anyAnnouncement = mockedAnnouncement()

        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        val textPlayer = initTextPlayer()

        every { mockedTextToSpeech.isLanguageAvailable(Locale(newLanguage)) } returns LANG_AVAILABLE

        verify(exactly = 1) {
            mockedTextToSpeech.setOnUtteranceProgressListener(any())
        }

        clearMocks(mockedTextToSpeech, answers = false)
        textPlayer.updateLanguage(newLanguage)

        verify(exactly = 1) {
            mockedTextToSpeech.language = Locale(newLanguage)
        }

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify(exactly = 1) { mockedTextToSpeech.speak(any(), any(), any(), any()) }
    }

    private fun TestScope.initTextPlayer(
        context: Context = mockk(relaxed = true),
        language: String = "en",
        playerAttributes: VoiceInstructionsPlayerAttributes = mockk(relaxed = true) {
            every { options } returns mockk(relaxed = true) {
                every { checkIsLanguageAvailable } returns true
            }
        },
        status: Int = TextToSpeech.SUCCESS,
    ): VoiceInstructionsTextPlayer {
        mockkObject(InternalJobControlFactory)
        val jobScope = CoroutineScope(this.coroutineContext + UnconfinedTestDispatcher())
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } answers {
            JobControl(jobScope.coroutineContext.job, jobScope)
        }

        return VoiceInstructionsTextPlayer(
            context,
            language,
            playerAttributes,
        ).also {
            invokeOnInitListener(status)
        }
    }

    private fun invokeOnInitListener(status: Int) {
        initListener.captured.onInit(status)
    }

    private fun mockedAnnouncement(): SpeechAnnouncement = mockk(relaxed = true) {
        every { announcement } returns "aaa"
    }
}
