package com.mapbox.navigation.ui.voice.internal.impl

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidanceServices
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidanceVoice
import com.mapbox.navigation.ui.voice.internal.MapboxVoiceInstructions

class MapboxAudioGuidanceServicesImpl : MapboxAudioGuidanceServices {

    override fun mapboxAudioGuidanceVoice(
        mapboxNavigation: MapboxNavigation,
        language: String,
    ): MapboxAudioGuidanceVoice {
        val mapboxSpeechApi = mapboxSpeechApi(mapboxNavigation, language)
        val mapboxVoiceInstructionsPlayer =
            mapboxVoiceInstructionsPlayer(mapboxNavigation, language)
        return MapboxAudioGuidanceVoice(
            mapboxSpeechApi,
            mapboxVoiceInstructionsPlayer
        )
    }

    override fun mapboxSpeechApi(
        mapboxNavigation: MapboxNavigation,
        language: String
    ): MapboxSpeechApi {
        val applicationContext = mapboxNavigation.navigationOptions.applicationContext
        val accessToken = mapboxNavigation.navigationOptions.accessToken!!
        return MapboxSpeechApi(applicationContext, accessToken, language)
    }

    override fun mapboxVoiceInstructionsPlayer(
        mapboxNavigation: MapboxNavigation,
        language: String,
    ): MapboxVoiceInstructionsPlayer {
        val applicationContext = mapboxNavigation.navigationOptions.applicationContext
        val accessToken = mapboxNavigation.navigationOptions.accessToken!!
        return MapboxVoiceInstructionsPlayer(applicationContext, accessToken, language)
    }

    override fun mapboxVoiceInstructions() = MapboxVoiceInstructions()
}
