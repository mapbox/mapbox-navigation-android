package com.mapbox.navigation.mapgpt.core.music

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import com.mapbox.navigation.mapgpt.core.MapGptCapabilities
import com.mapbox.navigation.mapgpt.core.Middleware

interface MusicPlayerMiddleware :
    Middleware<MusicPlayerContext>,
    MusicPlayer,
    MapGptCapabilities
{
    fun createContext(androidContext: Context): MusicPlayerContext?

    /**
     * [ActivityResultCaller] must be called early in the lifecycle of the activity or fragment.
     * This function makes it possible to register the launcher before making the request later.
     */
    fun registerPermissionLauncher(
        musicPlayerContext: MusicPlayerContext,
        activityResultCaller: ActivityResultCaller,
    )

    fun unregister(activityResultCaller: ActivityResultCaller)
}
