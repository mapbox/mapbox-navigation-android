package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer

internal class AudioGuidanceServices {

    fun audioGuidanceVoice(
        mapboxNavigation: MapboxNavigation,
        language: String,
    ): AudioGuidanceVoice {
        val mapboxSpeechApi = mapboxSpeechApi(mapboxNavigation, language)
        val mapboxVoiceInstructionsPlayer =
            mapboxVoiceInstructionsPlayer(mapboxNavigation, language)
        return AudioGuidanceVoice(
            mapboxSpeechApi,
            mapboxVoiceInstructionsPlayer
        )
    }

    private fun mapboxSpeechApi(
        mapboxNavigation: MapboxNavigation,
        language: String
    ): MapboxSpeechApi {
        val applicationContext = mapboxNavigation.navigationOptions.applicationContext
        val accessToken = mapboxNavigation.navigationOptions.accessToken!!
        return MapboxSpeechApi(applicationContext, accessToken, language)
    }

    private fun mapboxVoiceInstructionsPlayer(
        mapboxNavigation: MapboxNavigation,
        language: String,
    ): MapboxVoiceInstructionsPlayer {
        val applicationContext = mapboxNavigation.navigationOptions.applicationContext
        val accessToken = mapboxNavigation.navigationOptions.accessToken!!
        return MapboxVoiceInstructionsPlayer(applicationContext, accessToken, language)
    }
}
