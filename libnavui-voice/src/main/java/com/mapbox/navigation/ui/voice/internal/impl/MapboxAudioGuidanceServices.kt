package com.mapbox.navigation.ui.voice.internal.impl

import android.content.Context
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.utils.internal.configuration.NavigationConfigOwner
import com.mapbox.navigation.ui.utils.internal.datastore.NavigationDataStoreOwner
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidanceVoice
import com.mapbox.navigation.ui.voice.internal.MapboxVoiceInstructions
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions

class MapboxAudioGuidanceServices {

    var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null
        private set

    internal fun setVoiceInstructionsPlayer(player: MapboxVoiceInstructionsPlayer) {
        voiceInstructionsPlayer = player
    }

    fun mapboxAudioGuidanceVoice(
        mapboxNavigation: MapboxNavigation,
        language: String,
        options: MapboxSpeechApiOptions,
    ): MapboxAudioGuidanceVoice {
        val mapboxSpeechApi = mapboxSpeechApi(mapboxNavigation, language, options)
        val mapboxVoiceInstructionsPlayer =
            getOrUpdateMapboxVoiceInstructionsPlayer(mapboxNavigation, language)
        return MapboxAudioGuidanceVoice(
            mapboxSpeechApi,
            mapboxVoiceInstructionsPlayer
        )
    }

    fun mapboxSpeechApi(
        mapboxNavigation: MapboxNavigation,
        language: String,
        options: MapboxSpeechApiOptions
    ): MapboxSpeechApi {
        val applicationContext = mapboxNavigation.navigationOptions.applicationContext
        val accessToken = mapboxNavigation.navigationOptions.accessToken!!
        return MapboxSpeechApi(applicationContext, accessToken, language, options)
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
