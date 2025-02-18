package com.mapbox.navigation.mapgpt.core.wakeword

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.Middleware
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import com.mapbox.navigation.mapgpt.core.MapGptUserSettingsKeys.WAKEWORD_PROVIDER
import com.mapbox.navigation.mapgpt.core.common.MapGptEvents
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.common.Status
import com.mapbox.navigation.mapgpt.core.common.getString
import com.mapbox.navigation.mapgpt.core.common.setString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

interface WakeWordManager : Middleware<MapGptCoreContext> {
    val availableProviders: Set<WakeWordProvider>
    val selectedProvider: StateFlow<WakeWordProvider>
    val middleware: WakeWordMiddleware
    fun setDefaultProvider()
    fun setProvider(provider: WakeWordProvider)
}

object NoWakeWordManager : WakeWordManager {
    override val availableProviders: Set<WakeWordProvider> = setOf(WakeWordProvider.None)
    override val selectedProvider: StateFlow<WakeWordProvider> =
        MutableStateFlow(WakeWordProvider.None)
    override val middleware = NoWakeWordMiddleware
    override fun setDefaultProvider() = Unit
    override fun setProvider(provider: WakeWordProvider) = Unit
    override fun onAttached(middlewareContext: MapGptCoreContext) = Unit
    override fun onDetached(middlewareContext: MapGptCoreContext) = Unit
}

open class WakeWordMiddlewareManager(
    private val availableMiddleware: Set<WakeWordMiddleware>,
) : WakeWordManager, CoroutineMiddleware<MapGptCoreContext>() {

    override val availableProviders: Set<WakeWordProvider> =
        availableMiddleware.map { it.provider }.toSet()

    private val middlewareSwitcher = WakeWordMiddlewareSwitcher(availableMiddleware.first())
    override val middleware: WakeWordMiddleware = middlewareSwitcher

    override val selectedProvider: StateFlow<WakeWordProvider> =
        middlewareSwitcher.stateFlowOf { provider }

    override fun setDefaultProvider() {
        middlewareSwitcher.unregisterMiddleware()
        middlewareContext?.userSettings?.setString(WAKEWORD_PROVIDER, middlewareSwitcher.provider.key)
    }

    override fun setProvider(provider: WakeWordProvider) {
        availableMiddleware.firstOrNull { it.provider == provider }?.let { middleware ->
            middlewareSwitcher.registerMiddleware(middleware)
            middlewareContext?.userSettings?.setString(WAKEWORD_PROVIDER, provider.key)
        } ?: run {
            SharedLog.e(TAG) { "$provider is not available" }
            middlewareContext?.userSettings?.erase(WAKEWORD_PROVIDER)
            middlewareSwitcher.unregisterMiddleware()
        }
    }

    override fun onAttached(middlewareContext: MapGptCoreContext) {
        super.onAttached(middlewareContext)
        SharedLog.d(TAG) { "onAttached" }
        val lastUsedProvider = middlewareContext.userSettings.getString(WAKEWORD_PROVIDER)
        availableProviders.firstOrNull { it.key == lastUsedProvider }?.let { provider ->
            setProvider(provider)
        }
        middlewareSwitcher.onAttached(middlewareContext)
        middlewareContext.launchEnabledOrDisabledEvents()
    }

    override fun onDetached(middlewareContext: MapGptCoreContext) {
        super.onDetached(middlewareContext)
        middlewareSwitcher.onDetached(middlewareContext)
    }

    private fun MapGptCoreContext.launchEnabledOrDisabledEvents() {
        mainScope.launch(Dispatchers.Default) {
            observeWakeWordEnabled()
                .combine(middlewareSwitcher.middlewareState) { isEnabled, middleware ->
                    isEnabled to middleware.provider.key
                }.collectLatest { (isEnabled, provider) ->
                    when (isEnabled) {
                        true -> MapGptEvents.wakeWord(
                            Status.Enabled,
                            provider = provider,
                        )

                        false -> MapGptEvents.wakeWord(
                            Status.Disabled,
                            provider = provider,
                        )

                        null -> {
                            // Ignore null because state is not ready
                        }
                    }
                }
        }
    }

    companion object {
        private const val TAG = "WakeWordMiddlewareManager"
    }
}
