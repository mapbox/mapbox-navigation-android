package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class VoiceInstructionsTextPlayerTest {

    private val mockedBundle: Bundle = mockk(relaxUnitFun = true)

    @Before
    fun setUp() {
        mockkObject(BundleProvider)
        every { BundleProvider.retrieveBundle() } returns mockedBundle
    }

    @After
    fun tearDown() {
        unmockkObject(BundleProvider)
    }

    @Test(expected = IllegalStateException::class)
    fun `only one announcement can be played at a time`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        textPlayer.currentPlay = anyAnnouncement

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)
    }

    @Test
    fun `language is not supported current play is null`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = false

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `language is not supported and checkIsLanguageAvailable set to false current play not null`() {
        val anyContext = mockk<Context>(relaxed = true)
        val locale = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxed = true)
        every { anyPlayerAttributes.options } returns VoiceInstructionsPlayerOptions.Builder()
            .checkIsLanguageAvailable(false)
            .build()

        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, locale, anyPlayerAttributes)

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
        textPlayer.initializeWithLanguage(Locale.ENGLISH)

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

        assertEquals(anyAnnouncement, textPlayer.currentPlay)
    }

    @Test
    fun `language is not supported onDone is called`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = false

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify {
            anyVoiceInstructionsPlayerCallback.onDone(anyAnnouncement)
        }
    }

    @Test
    fun `announcement from state is blank current play is null`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val blankAnnouncement = mockk<SpeechAnnouncement>()
        every { blankAnnouncement.announcement } returns "   "
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = true

        textPlayer.play(blankAnnouncement, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `announcement from state is blank onDone is called`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
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
    fun `putFloat with volume level is called when play`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val anyNonBlankAnnouncement = mockk<SpeechAnnouncement>()
        every { anyNonBlankAnnouncement.announcement } returns "Turn right."
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = true

        textPlayer.play(anyNonBlankAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify { mockedBundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) }
    }

    @Test
    fun `player attributes applyOn with text to speech and bundle is called when play`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val anyNonBlankAnnouncement = mockk<SpeechAnnouncement>()
        every { anyNonBlankAnnouncement.announcement } returns "Turn right."
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = true
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.play(anyNonBlankAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify { anyPlayerAttributes.applyOn(mockedTextToSpeech, mockedBundle) }
    }

    @Test
    fun `text to speech speak with announcement and bundle is called when play`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val turnRightAnnouncement = "Turn right."
        val anyNonBlankAnnouncement = mockk<SpeechAnnouncement>()
        every { anyNonBlankAnnouncement.announcement } returns turnRightAnnouncement
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = true
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.play(anyNonBlankAnnouncement, anyVoiceInstructionsPlayerCallback)

        verify {
            textPlayer.textToSpeech.speak(
                turnRightAnnouncement,
                TextToSpeech.QUEUE_FLUSH,
                mockedBundle,
                "default_id"
            )
        }
    }

    @Test
    fun `volume level is updated when volume`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val aSpeechVolume = SpeechVolume(0.5f)

        textPlayer.volume(aSpeechVolume)

        assertEquals(0.5f, textPlayer.volumeLevel)
    }

    @Test
    fun `if announcing and volume is muted text to speech stop is called when volume`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val mute = SpeechVolume(0.0f)
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        every { mockedTextToSpeech.isSpeaking } returns true
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.volume(mute)

        verify { textPlayer.textToSpeech.stop() }
    }

    @Test
    fun `text to speech stop is called is called when clear`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.clear()

        verify {
            textPlayer.textToSpeech.stop()
        }
    }

    @Test
    fun `current play null after clear`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.clear()

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `text to speech setOnUtteranceProgressListener with null is called when shutdown`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.shutdown()

        verify {
            textPlayer.textToSpeech.setOnUtteranceProgressListener(null)
        }
    }

    @Test
    fun `text to speech shutdown is called when shutdown`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.shutdown()

        verify {
            textPlayer.textToSpeech.shutdown()
        }
    }

    @Test
    fun `current play is null after shutdown`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.shutdown()

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `volume level is reset to default after shutdown`() {
        val anyContext = mockk<Context>(relaxed = true)
        val language = "en"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, language, anyPlayerAttributes)
        textPlayer.volumeLevel = 0.5f
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.shutdown()

        assertEquals(1.0f, textPlayer.volumeLevel)
    }
}
