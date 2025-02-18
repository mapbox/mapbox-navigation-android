package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.common.randomUUID

/**
 * Construct that defines the [text] to be played using the [voice] injected.
 *
 * @param text the text to be played
 * @param voice to be used to play the [text]
 */
sealed class Announcement(open val text: String, open val voice: Voice) : AudioMixer.Clip {

    /**
     * Unique ID of the utterance.
     */
    val utteranceId: String = randomUUID()

    /**
     * Caching ID. Allows [Announcement] to be stored as unique transcriptions.
     */
    val mediaCacheId: String by lazy {
        text.plus("_")
            .plus(voice::class.qualifiedName).plus("_")
            .plus(voice.hashCode()).hashCode()
            .toString()
    }

    /**
     * Announcement that is played if nothing else is being played, or is queued otherwise.
     */
    data class Regular(
        override val text: String,
        override val voice: Voice,
    ) : Announcement(text, voice) {
        override val track: Int = AudioMixer.TRACK_REGULAR

        override fun toString(): String {
            return "Regular(${super.toString()})"
        }
    }

    /** Announcement that interrupts currently playing [Regular] messages
     * or is queued if another [Priority] message is being played.
     * Once the priority message finish, the [Regular] message queue is resumed.
     */
    data class Priority(
        override val text: String,
        override val voice: Voice,
    ) : Announcement(text, voice) {
        override val track: Int = AudioMixer.TRACK_PRIORITY
        override fun toString(): String {
            return "Priority(${super.toString()}"
        }
    }

    override fun toString(): String {
        return "utteranceId='$utteranceId', " +
            "mediaCacheId=$mediaCacheId, " +
            "text='$text', " +
            "voice=$voice"
    }
}
