package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement

/**
 * Represents the state of [MapboxAudioGuidance].
 */
class MapboxAudioGuidanceState internal constructor(
    /**
     * When the trip session has started, and there is an active route.
     */
    val isPlayable: Boolean = false,

    /**
     * Controlled by the [MapboxAudioGuidance] service. When [MapboxAudioGuidance.mute] is called
     * this will be changed to true.
     */
    val isMuted: Boolean = false,

    /**
     * Whether the `voiceInstructions` is the first one for this route.
     * You can set different behaviour based on this information.
     * For example, you can skip the starter instruction or always wait until it is downloaded
     * instead of using onboard fallback.
     */
    val isFirst: Boolean = false,

    /**
     * Once a voice instruction becomes available this will not be null.
     * When the state [isPlayable] and this is null, it means there are no voice instructions
     * on the route at this time.
     */
    val voiceInstructions: VoiceInstructions? = null,

    /**
     * After a [voiceInstructions] has been announced, this value will be emitted.
     * This will always be null when [isMuted] is true.
     */
    val speechAnnouncement: SpeechAnnouncement? = null,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxAudioGuidanceState

        if (isPlayable != other.isPlayable) return false
        if (isMuted != other.isMuted) return false
        if (isFirst != other.isFirst) return false
        if (voiceInstructions != other.voiceInstructions) return false
        if (speechAnnouncement != other.speechAnnouncement) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = isPlayable.hashCode()
        result = 31 * result + isMuted.hashCode()
        result = 31 * result + isFirst.hashCode()
        result = 31 * result + (voiceInstructions?.hashCode() ?: 0)
        result = 31 * result + (speechAnnouncement?.hashCode() ?: 0)
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "MapboxAudioGuidanceState(" +
            "isPlayable=$isPlayable, " +
            "isMuted=$isMuted, " +
            "isFirst=$isFirst, " +
            "voiceInstructions=$voiceInstructions, " +
            "speechAnnouncement=$speechAnnouncement" +
            ")"
    }
}
