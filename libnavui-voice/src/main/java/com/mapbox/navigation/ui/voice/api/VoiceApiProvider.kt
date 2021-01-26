package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts

internal object VoiceApiProvider {

    fun retrieveMapboxVoiceApi(
        context: Context,
        accessToken: String,
        language: String
    ): MapboxVoiceApi = MapboxVoiceApi(
        MapboxSpeechProvider(
            accessToken,
            language,
            MapboxNavigationAccounts.getInstance(context.applicationContext)
        ),
        MapboxSpeechFileProvider(context)
    )
}
