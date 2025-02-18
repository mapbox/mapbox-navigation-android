package com.mapbox.navigation.mapgpt.core.microphone

import android.util.Log
import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import com.mapbox.navigation.mapgpt.core.common.getString
import com.mapbox.navigation.mapgpt.core.common.setString
import kotlinx.coroutines.flow.StateFlow

open class MicrophoneMiddlewareManager(
    private val availableMiddleware: Set<PlatformMicrophoneMiddleware>,
) : CoroutineMiddleware<MapGptCoreContext>() {

    val availableProviders: Set<MicrophoneProvider> =
        availableMiddleware.map { it.provider }.toSet()

    private val middlewareSwitcher = MicrophoneMiddlewareSwitcher(availableMiddleware.first())
    val selectedProvider: StateFlow<MicrophoneProvider> =
        middlewareSwitcher.stateFlowOf { provider }

    val microphone: PlatformMicrophone = middlewareSwitcher

    fun setDefaultProvider() {
        middlewareSwitcher.unregisterMiddleware()
    }

    fun setProvider(provider: MicrophoneProvider) {
        availableMiddleware.firstOrNull { it.provider == provider }?.let { middleware ->
            middlewareSwitcher.registerMiddleware(middleware)
            middlewareContext?.userSettings?.setString(SETTING_MICROPHONE_PROVIDER, provider.key)
        } ?: run {
            Log.e(TAG, "$provider is not available")
            middlewareContext?.userSettings?.erase(SETTING_MICROPHONE_PROVIDER)
            middlewareSwitcher.unregisterMiddleware()
        }
    }

    override fun onAttached(middlewareContext: MapGptCoreContext) {
        super.onAttached(middlewareContext)
        val lastUsedProvider = middlewareContext.userSettings.getString(SETTING_MICROPHONE_PROVIDER)
        availableProviders.firstOrNull { it.key == lastUsedProvider }?.let { provider ->
            setProvider(provider)
        }
        middlewareSwitcher.onAttached(middlewareContext)
    }

    override fun onDetached(middlewareContext: MapGptCoreContext) {
        super.onDetached(middlewareContext)
        middlewareSwitcher.onDetached(middlewareContext)
    }

    companion object {
        private const val TAG = "MicrophoneMiddlewareManager"

        const val SETTING_MICROPHONE_PROVIDER = "microphone_provider"
    }
}
