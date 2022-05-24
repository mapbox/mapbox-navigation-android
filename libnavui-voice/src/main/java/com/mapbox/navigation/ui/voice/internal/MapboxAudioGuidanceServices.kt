package com.mapbox.navigation.ui.voice.internal

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer

interface MapboxAudioGuidanceServices {
    fun mapboxAudioGuidanceVoice(
        mapboxNavigation: MapboxNavigation,
        language: String
    ): MapboxAudioGuidanceVoice

    fun mapboxSpeechApi(mapboxNavigation: MapboxNavigation, language: String): MapboxSpeechApi
    fun mapboxVoiceInstructionsPlayer(
        mapboxNavigation: MapboxNavigation,
        language: String,
    ): MapboxVoiceInstructionsPlayer

    fun mapboxVoiceInstructions(): MapboxVoiceInstructions
}
