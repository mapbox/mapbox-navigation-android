package com.mapbox.navigation.mapgpt.core.music

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import com.mapbox.navigation.mapgpt.core.MapGptCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class NoMusicPlayerMiddleware : MusicPlayerMiddleware {
    override val provider: MusicPlayerProvider = MusicPlayerProvider.None
    override val capabilities: Flow<Set<MapGptCapability>> = flowOf(emptySet())
    override val playbackState: StateFlow<MusicPlayerState?> = MutableStateFlow(null)
    override fun createContext(androidContext: Context): MusicPlayerContext? = null

    override fun registerPermissionLauncher(
        musicPlayerContext: MusicPlayerContext,
        activityResultCaller: ActivityResultCaller,
    ) = Unit

    override fun unregister(activityResultCaller: ActivityResultCaller) = Unit

    override fun onAttached(middlewareContext: MusicPlayerContext) = Unit

    override fun onDetached(middlewareContext: MusicPlayerContext) = Unit

    override fun play(providerUri: String) = Unit

    override fun pause() = Unit

    override fun resume() = Unit

    override fun stop() = Unit
    override fun next() = Unit

    override fun previous() = Unit

    override fun setShuffleMode(mode: MusicPlayerState.ShuffleMode) = Unit

    override fun setRepeatMode(mode: MusicPlayerState.RepeatMode) = Unit

    override fun seek(position: Float) = Unit
}
