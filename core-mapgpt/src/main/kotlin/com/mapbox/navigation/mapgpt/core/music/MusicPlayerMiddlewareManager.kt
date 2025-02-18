package com.mapbox.navigation.mapgpt.core.music

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import com.mapbox.navigation.mapgpt.core.MapGptCapabilities
import com.mapbox.navigation.mapgpt.core.MapGptCapability
import com.mapbox.navigation.mapgpt.core.MiddlewareContext
import com.mapbox.navigation.mapgpt.core.MapGptCapabilitiesProvider
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Provides an entry point for managing the lifecycle of the [MusicPlayerMiddleware] and handling
 * music playback requests. In the future when you want to create multiple instances of the
 * [MusicPlayerMiddleware] you can update this interface to manage so that you can manage the
 * lifecycle and state changes of multiple music players.
 *
 * For example, if you pass in spotify and apple music middleware instances, you can make change
 * the interface to surface the music players and provide a way to switch between them.
 */
interface MusicPlayerMiddlewareManager : MusicPlayer, MiddlewareContext, MapGptCapabilities {
    val availableProviders: Set<MusicPlayerProvider>
    val currentProvider: StateFlow<MusicPlayerProvider>
    val musicPlayerContext: StateFlow<MusicPlayerContext?>
    fun registerPermissionLauncher(activityResultCaller: ActivityResultCaller)
    fun unregister(activityResultCaller: ActivityResultCaller)
    fun setProvider(provider: MusicPlayerProvider): Boolean
}

class NoMusicPlayerMiddlewareManager :
    MusicPlayerMiddlewareManager,
    MusicPlayer by NoMusicPlayerMiddleware() {

    override val capabilities: Flow<Set<MapGptCapability>> = flowOf(emptySet())
    override val availableProviders: Set<MusicPlayerProvider> = setOf(MusicPlayerProvider.None)
    override val currentProvider: StateFlow<MusicPlayerProvider> =
        MutableStateFlow(MusicPlayerProvider.None)
    override val musicPlayerContext = MutableStateFlow<MusicPlayerContext?>(null)
    override fun registerPermissionLauncher(activityResultCaller: ActivityResultCaller) = Unit
    override fun unregister(activityResultCaller: ActivityResultCaller) = Unit
    override fun setProvider(provider: MusicPlayerProvider) = false
}

class MultiMusicPlayerMiddlewareManager(
    private val androidContext: Context,
    private val availableMusicPlayers: Set<MusicPlayerMiddleware>,
    private val musicProviderKey: MutableStateFlow<String?> = MutableStateFlow(null),
) : MusicPlayerMiddlewareManager {
    override val availableProviders: Set<MusicPlayerProvider> by lazy {
        availableMusicPlayers.map { it.provider }.toSet()
    }
    private val musicPlayerMap: Map<MusicPlayerProvider, MusicPlayerMiddleware> by lazy {
        if (availableMusicPlayers.isEmpty()) {
            val noMusicPlayerMiddleware = NoMusicPlayerMiddleware()
            mapOf(noMusicPlayerMiddleware.provider to noMusicPlayerMiddleware)
        } else {
            availableMusicPlayers.associateBy { it.provider }
        }
    }
    private val musicContextMap: Map<MusicPlayerProvider, MusicPlayerContext?> by lazy {
        musicPlayerMap.values
            .associateBy { it.provider }
            .mapValues { it.value.createContext(androidContext) }
    }

    private val currentPlayer by lazy {
        MutableStateFlow(
            availableMusicPlayers.firstOrNull { it.provider.key == musicProviderKey.value }
                ?: availableMusicPlayers.first(),
        )
    }

    override val capabilities: StateFlow<Set<MapGptCapability>> by lazy {
        MapGptCapabilitiesProvider(stateScope, availableMusicPlayers).capabilities
    }

    override val provider: MusicPlayerProvider get() = currentPlayer.value.provider

    private val _playbackState = MutableStateFlow<MusicPlayerState?>(null)
    override val playbackState = _playbackState.asStateFlow()

    private val stateScope = MainScope()
    private lateinit var coroutineScope: CoroutineScope

    override fun setProvider(provider: MusicPlayerProvider): Boolean {
        SharedLog.d(TAG) { "setMusicPlayerProvider: $provider" }
        return musicPlayerMap[provider]?.let { middleware ->
            updatePlayerAsync(middleware)
            true
        } ?: false
    }

    override fun play(providerUri: String) {
        currentPlayer.value.play(providerUri)
    }

    override fun pause() {
        currentPlayer.value.pause()
    }

    override fun resume() {
        currentPlayer.value.resume()
    }

    override fun stop() {
        currentPlayer.value.stop()
    }

    override fun next() {
        currentPlayer.value.next()
    }

    override fun previous() {
        currentPlayer.value.previous()
    }

    override fun setShuffleMode(mode: MusicPlayerState.ShuffleMode) {
        currentPlayer.value.setShuffleMode(mode)
    }

    override fun setRepeatMode(mode: MusicPlayerState.RepeatMode) {
        currentPlayer.value.setRepeatMode(mode)
    }

    override fun seek(position: Float) {
        currentPlayer.value.seek(position)
    }

    override val musicPlayerContext: StateFlow<MusicPlayerContext?> by lazy {
        currentPlayer.map { musicContextMap[it.provider] }.stateIn(
            stateScope,
            SharingStarted.Eagerly,
            musicContextMap[currentPlayer.value.provider],
        )
    }

    override val currentProvider: StateFlow<MusicPlayerProvider> by lazy {
        currentPlayer.map { it.provider }.stateIn(
            stateScope,
            SharingStarted.Eagerly,
            currentPlayer.value.provider,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun registerPermissionLauncher(activityResultCaller: ActivityResultCaller) {
        SharedLog.d(TAG) { "registerPermissionLauncher" }
        val currentPlayerValue = currentPlayer.value
        musicContextMap[currentPlayerValue.provider]?.let { currentContext ->
            currentPlayerValue.onAttached(currentContext)
        }
        musicPlayerMap.forEach { (provider, middleware) ->
            musicContextMap[provider]?.let {
                middleware.registerPermissionLauncher(it, activityResultCaller)
            }
        }
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        currentPlayer.flatMapLatest { it.playbackState }.onEach { playbackState ->
            _playbackState.value = playbackState
        }.launchIn(coroutineScope)
    }

    override fun unregister(activityResultCaller: ActivityResultCaller) {
        SharedLog.d(TAG) { "unregister" }
        coroutineScope.cancel()
        musicPlayerMap.forEach { (provider, middleware) ->
            musicContextMap[provider]?.let {
                middleware.unregister(activityResultCaller)
            }
        }
        val currentPlayerValue = currentPlayer.value
        musicContextMap[currentPlayerValue.provider]?.let { currentContext ->
            currentPlayerValue.onDetached(currentContext)
        }
        _playbackState.value = null
    }

    /**
     * This method is called when the [MusicPlayerMiddleware] changes. It will ensure that flows
     * observing the [currentPlayer] can be detached before new middleware is being attached.
     */
    private fun updatePlayerAsync(middleware: MusicPlayerMiddleware) {
        val previousPlayer = currentPlayer.value
        coroutineScope.launch {
            musicContextMap[previousPlayer.provider]?.let { previousContext ->
                SharedLog.d(TAG) { "onDetached: ${previousPlayer.provider}" }
                previousPlayer.onDetached(previousContext)
            }
        }
        currentPlayer.value = NoMusicPlayerMiddleware()
        coroutineScope.launch {
            musicContextMap[middleware.provider]?.let { nextContext ->
                SharedLog.d(TAG) { "onAttached: ${middleware.provider}" }
                middleware.onAttached(nextContext)
            }
        }
        coroutineScope.launch {
            currentPlayer.value = middleware
            musicProviderKey.value = middleware.provider.key
        }
    }

    private companion object {
        private const val TAG = "MusicPlayerMiddlewareManager"
    }
}
