package com.mapbox.navigation.voice.api

import android.content.Context
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.ui.base.util.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.voice.options.MapboxSpeechApiOptions
import java.io.File

internal object VoiceApiProvider {

    private const val MAPBOX_INSTRUCTIONS_CACHE = "mapbox_instructions_cache"

    fun retrieveMapboxVoiceApi(
        context: Context,
        language: String,
        options: MapboxSpeechApiOptions,
    ): MapboxVoiceApi = MapboxVoiceApi(
        MapboxSpeechProvider(
            language,
            MapboxNavigationAccounts(),
            options,
            ResourceLoaderFactory.getInstance(),
        ),
        MapboxSpeechFileProvider(
            File(
                context.applicationContext.cacheDir,
                MAPBOX_INSTRUCTIONS_CACHE,
            ),
        ),
    )
}
