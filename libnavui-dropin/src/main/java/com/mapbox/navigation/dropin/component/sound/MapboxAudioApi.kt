package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.extensions.flowVoiceInstructions
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MapboxAudioApi private constructor(
    val mapboxNavigation: MapboxNavigation
) {
    fun speakVoiceInstructions(): Flow<SpeechAnnouncement?> {
        // TODO continuously speak the announcement while attached
        return mapboxNavigation.flowVoiceInstructions().map { null }
    }

    companion object {
        fun create(mapboxNavigation: MapboxNavigation) = MapboxAudioApi(mapboxNavigation)
    }
}
