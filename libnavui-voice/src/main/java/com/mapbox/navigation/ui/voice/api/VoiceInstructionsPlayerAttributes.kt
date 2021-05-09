package com.mapbox.navigation.ui.voice.api

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresApi
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions

/**
 * PlayerAttributes implements attributes that define how the audio system handles routing
 * and focus decisions for the specified source.
 */
sealed class VoiceInstructionsPlayerAttributes {

    /**
     * VoiceInstructionsPlayerOptions.
     */
    abstract val options: VoiceInstructionsPlayerOptions

    /**
     * Configure [MediaPlayer]
     */
    internal fun applyOn(mediaPlayer: MediaPlayer) {
        configureMediaPlayer()(mediaPlayer)
    }

    /**
     * Configure [TextToSpeech]
     */
    internal fun applyOn(textToSpeech: TextToSpeech, bundle: Bundle) {
        configureTextToSpeech()(textToSpeech, bundle)
    }

    /**
     * Configure [AudioFocusRequest.Builder]
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    internal fun applyOn(audioFocusRequestBuilder: AudioFocusRequest.Builder) {
        configureAudioFocusRequestBuilder()(audioFocusRequestBuilder)
    }

    protected abstract fun configureMediaPlayer(): MediaPlayer.() -> Unit
    protected abstract fun configureTextToSpeech(): TextToSpeech.(Bundle) -> Unit
    protected abstract fun configureAudioFocusRequestBuilder(): AudioFocusRequest.Builder.() -> Unit

    /**
     * Attributes for API below Android O
     */
    internal data class PreOreoAttributes(
        override val options: VoiceInstructionsPlayerOptions,
    ) : VoiceInstructionsPlayerAttributes() {

        override fun configureMediaPlayer(): MediaPlayer.() -> Unit {
            return {
                setAudioStreamType(options.streamType)
            }
        }

        override fun configureTextToSpeech(): TextToSpeech.(Bundle) -> Unit {
            return { bundle ->
                bundle.putString(
                    TextToSpeech.Engine.KEY_PARAM_STREAM,
                    options.streamType.toString()
                )
            }
        }

        override fun configureAudioFocusRequestBuilder(): AudioFocusRequest.Builder.() -> Unit {
            return {
                // Not used
            }
        }
    }

    /**
     * Attributes for API Android O and above
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    internal data class OreoAndLaterAttributes(
        override val options: VoiceInstructionsPlayerOptions,
    ) : VoiceInstructionsPlayerAttributes() {

        /**
         * Specifies a collection of attributes describing information about an audio stream.
         */
        private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .let { builder ->
                if (options.useLegacyApi) {
                    builder.buildLegacy()
                } else {
                    builder.buildNormal()
                }
            }
            .build()

        override fun configureMediaPlayer(): MediaPlayer.() -> Unit {
            return {
                setAudioAttributes(audioAttributes)
            }
        }

        override fun configureTextToSpeech(): TextToSpeech.(Bundle) -> Unit {
            return {
                setAudioAttributes(audioAttributes)
            }
        }

        override fun configureAudioFocusRequestBuilder(): AudioFocusRequest.Builder.() -> Unit {
            return {
                setAudioAttributes(audioAttributes)
            }
        }

        private fun AudioAttributes.Builder.buildNormal() = setUsage(options.usage)
            .setContentType(options.contentType)

        private fun AudioAttributes.Builder.buildLegacy() = setLegacyStreamType(options.streamType)
    }
}
