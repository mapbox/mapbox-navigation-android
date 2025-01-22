package com.mapbox.navigation.voice.internal

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.voice.api.MapboxAudioGuidanceState
import com.mapbox.navigation.voice.model.SpeechAnnouncement

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object MapboxAudioGuidanceStateFactory {

    @JvmSynthetic
    fun createMapboxAudioGuidanceState(
        isPlayable: Boolean = false,
        isMuted: Boolean = false,
        isFirst: Boolean = false,
        voiceInstructions: VoiceInstructions? = null,
        speechAnnouncement: SpeechAnnouncement? = null,
    ) = MapboxAudioGuidanceState(
        isPlayable = isPlayable,
        isMuted = isMuted,
        isFirst = isFirst,
        voiceInstructions = voiceInstructions,
        speechAnnouncement = speechAnnouncement,
    )
}
