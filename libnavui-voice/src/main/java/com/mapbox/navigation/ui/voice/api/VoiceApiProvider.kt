package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import java.io.File

internal object VoiceApiProvider {

    private const val MAPBOX_INSTRUCTIONS_CACHE = "mapbox_instructions_cache"

    fun retrieveMapboxVoiceApi(
        context: Context,
        accessToken: String,
        language: String,
        options: MapboxSpeechApiOptions
    ): MapboxVoiceApi = MapboxVoiceApi(
        MapboxSpeechProvider(
            accessToken,
            language,
            MapboxNavigationAccounts,
            options
        ),
        MapboxSpeechFileProvider(
            File(
                context.applicationContext.cacheDir,
                MAPBOX_INSTRUCTIONS_CACHE
            )
        )
    )
}
