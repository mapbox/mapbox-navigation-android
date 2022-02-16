package com.mapbox.navigation.dropin.component.sound

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement

/**
 * @param isMuted user controlled value to mute or un-mute audio guidance
 * @param voiceInstructions the current voice instructions
 * @param speechAnnouncement the current speech announcements
 */
@ExperimentalPreviewMapboxNavigationAPI
data class MapboxAudioState internal constructor(
    val isMuted: Boolean = false,
    val voiceInstructions: VoiceInstructions? = null,
    val speechAnnouncement: SpeechAnnouncement? = null,
)
