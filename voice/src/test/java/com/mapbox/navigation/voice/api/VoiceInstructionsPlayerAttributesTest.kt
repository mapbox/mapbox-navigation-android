package com.mapbox.navigation.voice.api

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.mapbox.navigation.voice.model.AudioFocusOwner
import com.mapbox.navigation.voice.options.VoiceInstructionsPlayerOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class VoiceInstructionsPlayerAttributesTest {

    @Test
    fun `PreOreoAttributes configureTextToSpeech must set stream type`() {
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.ttsStreamType
        } returns AudioManager.STREAM_MUSIC

        val attributes = VoiceInstructionsPlayerAttributes.PreOreoAttributes(mockedPlayerOptions)

        val bundle: Bundle = mockk(relaxed = true)
        val tts: TextToSpeech = mockk(relaxed = true)
        attributes.applyOn(tts, bundle)

        verify(exactly = 1) {
            bundle.putString(
                TextToSpeech.Engine.KEY_PARAM_STREAM,
                AudioManager.STREAM_MUSIC.toString(),
            )
        }

        verify(exactly = 0) {
            tts.setAudioAttributes(any())
        }
    }

    @Test
    fun `PreOreoAttributes configureMediaPlayer must set stream type`() {
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        val attributes = VoiceInstructionsPlayerAttributes.PreOreoAttributes(mockedPlayerOptions)

        val mediaPlayer: MediaPlayer = mockk(relaxed = true)
        attributes.applyOn(mediaPlayer)

        verify(exactly = 1) {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
    }

    @Test
    fun `PreOreoAttributes configureAudioFocusRequestBuilder never used`() {
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        val attributes = VoiceInstructionsPlayerAttributes.PreOreoAttributes(mockedPlayerOptions)

        val audioFocusRequest: AudioFocusRequest.Builder = mockk(relaxed = true)
        val mockOwner: AudioFocusOwner = mockk()
        attributes.applyOn(mockOwner, audioFocusRequest)

        verify(exactly = 0) {
            audioFocusRequest.setFocusGain(any())
        }

        verify(exactly = 0) {
            audioFocusRequest.setAudioAttributes(any())
        }

        verify(exactly = 0) {
            audioFocusRequest.build()
        }
    }

    @Test
    fun `OreoAndLaterAttributes configureTextToSpeech must call setAudioAttributes`() {
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.ttsStreamType
        } returns AudioManager.STREAM_MUSIC

        every {
            mockedPlayerOptions.useLegacyApi
        } returns true

        val audioAttributes: AudioAttributes = mockk(relaxed = true)
        val audioAttributesBuilder: AudioAttributes.Builder = mockk()

        every {
            audioAttributesBuilder.build()
        } returns audioAttributes

        every {
            audioAttributesBuilder.setLegacyStreamType(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setUsage(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setContentType(any())
        } returns audioAttributesBuilder

        val attributes = VoiceInstructionsPlayerAttributes.OreoAndLaterAttributes(
            mockedPlayerOptions,
            audioAttributesBuilder,
        )

        val bundle: Bundle = mockk(relaxed = true)
        val tts: TextToSpeech = mockk(relaxed = true)
        attributes.applyOn(tts, bundle)

        verify(exactly = 1) {
            tts.setAudioAttributes(audioAttributes)
        }

        verify(exactly = 0) {
            bundle.putInt(any(), any())
        }

        verify(exactly = 0) {
            bundle.putString(any(), any())
        }
    }

    @Test
    fun `OreoAndLaterAttributes configureMediaPlayer must set audioAttributes`() {
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        every {
            mockedPlayerOptions.useLegacyApi
        } returns true

        val audioAttributes: AudioAttributes = mockk(relaxed = true)
        val audioAttributesBuilder: AudioAttributes.Builder = mockk()

        every {
            audioAttributesBuilder.build()
        } returns audioAttributes

        every {
            audioAttributesBuilder.setLegacyStreamType(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setUsage(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setContentType(any())
        } returns audioAttributesBuilder

        val attributes = VoiceInstructionsPlayerAttributes.OreoAndLaterAttributes(
            mockedPlayerOptions,
            audioAttributesBuilder,
        )

        val mediaPlayer: MediaPlayer = mockk(relaxed = true)
        attributes.applyOn(mediaPlayer)

        verify(exactly = 1) {
            mediaPlayer.setAudioAttributes(audioAttributes)
        }

        verify(exactly = 0) {
            mediaPlayer.setAudioStreamType(any())
        }
    }

    @Test
    fun `OreoAndLaterAttributes configureAudioFocusRequestBuilder must set audioAttributes`() {
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        every {
            mockedPlayerOptions.useLegacyApi
        } returns true

        val audioAttributes: AudioAttributes = mockk(relaxed = true)
        val audioAttributesBuilder: AudioAttributes.Builder = mockk()

        every {
            audioAttributesBuilder.build()
        } returns audioAttributes

        every {
            audioAttributesBuilder.setLegacyStreamType(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setUsage(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setContentType(any())
        } returns audioAttributesBuilder

        val attributes = VoiceInstructionsPlayerAttributes.OreoAndLaterAttributes(
            mockedPlayerOptions,
            audioAttributesBuilder,
        )

        val audioFocusRequest: AudioFocusRequest.Builder = mockk(relaxed = true)
        attributes.applyOn(AudioFocusOwner.MediaPlayer, audioFocusRequest)

        verify(exactly = 1) {
            audioFocusRequest.setAudioAttributes(audioAttributes)
        }

        verify(exactly = 0) {
            audioFocusRequest.setFocusGain(any())
        }
    }

    @Test
    fun `OreoAndLaterAttributes when use legacy api should buildLegacy`() {
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        every {
            mockedPlayerOptions.useLegacyApi
        } returns true

        val audioAttributes: AudioAttributes = mockk(relaxed = true)
        val audioAttributesBuilder: AudioAttributes.Builder = mockk()

        every {
            audioAttributesBuilder.build()
        } returns audioAttributes

        every {
            audioAttributesBuilder.setLegacyStreamType(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setUsage(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setContentType(any())
        } returns audioAttributesBuilder

        val attributes = VoiceInstructionsPlayerAttributes.OreoAndLaterAttributes(
            mockedPlayerOptions,
            audioAttributesBuilder,
        )

        attributes.audioAttributes(AudioFocusOwner.MediaPlayer)

        verify(exactly = 1) {
            audioAttributesBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC)
        }

        verify(exactly = 0) {
            audioAttributesBuilder.setContentType(any())
        }

        verify(exactly = 0) {
            audioAttributesBuilder.setUsage(any())
        }
    }

    @Test
    fun `OreoAndLaterAttributes when do not use legacy api should buildNormal`() {
        val mockedPlayerOptions: VoiceInstructionsPlayerOptions = mockk()
        every {
            mockedPlayerOptions.streamType
        } returns AudioManager.STREAM_MUSIC

        every {
            mockedPlayerOptions.usage
        } returns AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE

        every {
            mockedPlayerOptions.contentType
        } returns AudioAttributes.CONTENT_TYPE_MUSIC

        every {
            mockedPlayerOptions.useLegacyApi
        } returns false

        val audioAttributes: AudioAttributes = mockk(relaxed = true)
        val audioAttributesBuilder: AudioAttributes.Builder = mockk()

        every {
            audioAttributesBuilder.build()
        } returns audioAttributes

        every {
            audioAttributesBuilder.setLegacyStreamType(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setUsage(any())
        } returns audioAttributesBuilder

        every {
            audioAttributesBuilder.setContentType(any())
        } returns audioAttributesBuilder

        val attributes = VoiceInstructionsPlayerAttributes.OreoAndLaterAttributes(
            mockedPlayerOptions,
            audioAttributesBuilder,
        )

        attributes.audioAttributes(AudioFocusOwner.MediaPlayer)

        verify(exactly = 0) {
            audioAttributesBuilder.setLegacyStreamType(any())
        }

        verify(exactly = 1) {
            audioAttributesBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        }

        verify(exactly = 1) {
            audioAttributesBuilder.setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        }
    }
}
