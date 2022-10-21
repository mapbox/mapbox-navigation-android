package com.mapbox.navigation.ui.voice.internal.impl

import android.content.Context
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.utils.internal.configuration.NavigationConfigOwner
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidanceVoice
import com.mapbox.navigation.ui.voice.internal.MapboxVoiceInstructions

class MapboxAudioGuidanceServices {

    var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null
        private set

    fun mapboxAudioGuidanceVoice(
        mapboxNavigation: MapboxNavigation,
        language: String,
    ): MapboxAudioGuidanceVoice {
        val mapboxSpeechApi = mapboxSpeechApi(mapboxNavigation, language)
        val mapboxVoiceInstructionsPlayer =
            getOrUpdateMapboxVoiceInstructionsPlayer(mapboxNavigation, language)
        return MapboxAudioGuidanceVoice(
            mapboxSpeechApi,
            mapboxVoiceInstructionsPlayer
        )
    }

    fun mapboxSpeechApi(
        mapboxNavigation: MapboxNavigation,
        language: String
    ): MapboxSpeechApi {
        val applicationContext = mapboxNavigation.navigationOptions.applicationContext
        val accessToken = mapboxNavigation.navigationOptions.accessToken!!
        return MapboxSpeechApi(applicationContext, accessToken, language)
    }

    fun getOrUpdateMapboxVoiceInstructionsPlayer(
        mapboxNavigation: MapboxNavigation,
        language: String,
    ): MapboxVoiceInstructionsPlayer {
        return voiceInstructionsPlayer?.apply { updateLanguage(language) } ?: run {
            val applicationContext = mapboxNavigation.navigationOptions.applicationContext
            return MapboxVoiceInstructionsPlayer(applicationContext, language).also {
                voiceInstructionsPlayer = it
            }
        }
    }

    fun mapboxVoiceInstructions() = MapboxVoiceInstructions()

    fun configOwner(context: Context): NavigationConfigOwner = NavigationConfigOwner(context)

    fun dataStoreOwner(context: Context) = NavigationDataStoreOwner(context)
}
