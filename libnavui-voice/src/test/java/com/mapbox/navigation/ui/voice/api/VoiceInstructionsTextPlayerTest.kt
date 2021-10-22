package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
        val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)

        textPlayer.currentPlay = anyAnnouncement

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)
    }

    @Test
    fun `language is not supported current play is null`() {
        val anyContext = mockk<Context>(relaxed = true)
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
        val anyAnnouncement = mockk<SpeechAnnouncement>(relaxed = true)
        val anyVoiceInstructionsPlayerCallback =
            mockk<VoiceInstructionsPlayerCallback>(relaxUnitFun = true)
        textPlayer.isLanguageSupported = false

        textPlayer.play(anyAnnouncement, anyVoiceInstructionsPlayerCallback)

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `language is not supported onDone is called`() {
        val anyContext = mockk<Context>(relaxed = true)
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>(relaxUnitFun = true)
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
        val aSpeechVolume = SpeechVolume(0.5f)

        textPlayer.volume(aSpeechVolume)

        assertEquals(0.5f, textPlayer.volumeLevel)
    }

    @Test
    fun `if announcing and volume is muted text to speech stop is called when volume`() {
        val anyContext = mockk<Context>(relaxed = true)
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.clear()

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `text to speech setOnUtteranceProgressListener with null is called when shutdown`() {
        val anyContext = mockk<Context>(relaxed = true)
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
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
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.shutdown()

        assertEquals(null, textPlayer.currentPlay)
    }

    @Test
    fun `volume level is reset to default after shutdown`() {
        val anyContext = mockk<Context>(relaxed = true)
        val anyAccessToken = "pk.1234"
        val anyPlayerAttributes = mockk<VoiceInstructionsPlayerAttributes>()
        val textPlayer =
            VoiceInstructionsTextPlayer(anyContext, anyAccessToken, anyPlayerAttributes)
        textPlayer.volumeLevel = 0.5f
        val mockedTextToSpeech = mockk<TextToSpeech>(relaxed = true)
        textPlayer.textToSpeech = mockedTextToSpeech

        textPlayer.shutdown()

        assertEquals(1.0f, textPlayer.volumeLevel)
    }
}
