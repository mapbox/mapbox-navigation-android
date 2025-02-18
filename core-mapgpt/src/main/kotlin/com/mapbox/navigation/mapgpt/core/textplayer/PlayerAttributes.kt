package com.mapbox.navigation.mapgpt.core.textplayer

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresApi
import com.mapbox.navigation.mapgpt.core.audiofocus.AudioFocusOwner
import com.mapbox.navigation.mapgpt.core.textplayer.options.PlayerOptions
import org.jetbrains.annotations.TestOnly

/**
 * PlayerAttributes implements attributes that define how the audio system handles routing
 * and focus decisions for the specified source.
 */
internal sealed class PlayerAttributes {

    /**
     * PlayerOptions.
     */
    abstract val options: PlayerOptions

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
    internal fun applyOn(
        owner: AudioFocusOwner,
        audioFocusRequestBuilder: AudioFocusRequest.Builder,
    ) {
        configureAudioFocusRequestBuilder(owner)(audioFocusRequestBuilder)
    }

    protected abstract fun configureMediaPlayer(): MediaPlayer.() -> Unit
    protected abstract fun configureTextToSpeech(): TextToSpeech.(Bundle) -> Unit
    protected abstract fun configureAudioFocusRequestBuilder(
        owner: AudioFocusOwner,
    ): AudioFocusRequest.Builder.() -> Unit

    /**
     * Attributes for API below Android O
     */
    internal data class PreOreoAttributes(
        override val options: PlayerOptions,
    ) : PlayerAttributes() {

        override fun configureMediaPlayer(): MediaPlayer.() -> Unit {
            return {
                setAudioStreamType(options.streamType)
            }
        }

        override fun configureTextToSpeech(): TextToSpeech.(Bundle) -> Unit {
            return { bundle ->
                bundle.putString(
                    TextToSpeech.Engine.KEY_PARAM_STREAM,
                    options.ttsStreamType.toString(),
                )
            }
        }

        override fun configureAudioFocusRequestBuilder(
            owner: AudioFocusOwner,
        ): AudioFocusRequest.Builder.() -> Unit {
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
        override val options: PlayerOptions,
        val builder: AudioAttributes.Builder,
    ) : PlayerAttributes() {

        /**
         * Specifies a collection of attributes describing information about an audio stream.
         */
        @TestOnly
        internal fun audioAttributes(owner: AudioFocusOwner): AudioAttributes = builder
            .let { builder ->
                if (options.useLegacyApi) {
                    builder.buildLegacy(owner)
                } else {
                    builder.buildNormal()
                }
            }
            .build()

        override fun configureMediaPlayer(): MediaPlayer.() -> Unit {
            return {
                setAudioAttributes(audioAttributes(AudioFocusOwner.MediaPlayer))
            }
        }

        override fun configureTextToSpeech(): TextToSpeech.(Bundle) -> Unit {
            return {
                setAudioAttributes(audioAttributes(AudioFocusOwner.TextToSpeech))
            }
        }

        override fun configureAudioFocusRequestBuilder(
            owner: AudioFocusOwner,
        ): AudioFocusRequest.Builder.() -> Unit {
            return {
                setAudioAttributes(audioAttributes(owner))
            }
        }

        private fun AudioAttributes.Builder.buildNormal() = setUsage(options.usage)
            .setContentType(options.contentType)

        private fun AudioAttributes.Builder.buildLegacy(
            owner: AudioFocusOwner,
        ) = setLegacyStreamType(
            when (owner) {
                AudioFocusOwner.MediaPlayer -> options.streamType
                AudioFocusOwner.TextToSpeech -> options.ttsStreamType
                AudioFocusOwner.SpeechToText -> options.streamType
            },
        )
    }
}
