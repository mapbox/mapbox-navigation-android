package com.mapbox.navigation.mapgpt.core.wakeword

import com.mapbox.navigation.mapgpt.core.MapGptCore
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import com.mapbox.navigation.mapgpt.core.MapGptUserSettingsKeys.WAKEWORD_ENABLED
import com.mapbox.navigation.mapgpt.core.common.observeBoolean
import com.mapbox.navigation.mapgpt.core.settings.BooleanSettingEditor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion

fun MapGptCoreContext.observeWakeWordEnabled(): Flow<Boolean?> {
    return userSettings.observeBoolean(WAKEWORD_ENABLED)
}

fun MapGptCore.wakeWordEnabled(): BooleanSettingEditor {
    return BooleanSettingEditor(mapGptCore = this, key = WAKEWORD_ENABLED)
}

fun MapGptCore.getWakeWordManager(): WakeWordManager {
    return getMiddlewares(com.mapbox.navigation.mapgpt.core.wakeword.WakeWordMiddlewareManager::class).firstOrNull() ?: com.mapbox.navigation.mapgpt.core.wakeword.NoWakeWordManager
}

fun MapGptCore.observeWakeWordManager(): Flow<WakeWordManager> {
    return observeMiddlewares(com.mapbox.navigation.mapgpt.core.wakeword.WakeWordMiddlewareManager::class).map {
        it.firstOrNull() ?: com.mapbox.navigation.mapgpt.core.wakeword.NoWakeWordManager
    }
}

fun MapGptCore.setWakeWordProviders(middlewares: Set<WakeWordMiddleware>) = apply {
    getMiddlewares(com.mapbox.navigation.mapgpt.core.wakeword.WakeWordMiddlewareManager::class).forEach { previous ->
        unregister(previous)
    }
    if (middlewares.isNotEmpty()) {
        register(WakeWordMiddlewareManager(middlewares))
    }
}

/**
 * Uses a set of conditions to determine if the wake word system should listen. There is a user
 * preference to enable or disable wake word which is used regardless of the conditions.
 * @see [wakeWordEnabled]
 *
 * @param condition Condition that must be met for the wake word system to listen.
 * @param default The default preference when [wakeWordEnabled] is not set.
 */
suspend fun MapGptCore.listenForWakeWord(
    condition: Flow<Boolean>,
    default: Boolean = false,
) {
    combine(
        wakeWordEnabled().observe().mapNotNull { it ?: default },
        condition,
        observeWakeWordManager().map { it.middleware },
    ) { isEnabled, conditionValue, middleware ->
        (isEnabled && conditionValue) to middleware
    }.onCompletion {
        getWakeWordManager().middleware.stopListening()
    }.distinctUntilChanged().collectLatest { (isEnabled, middleware) ->
        if (isEnabled) {
            if (middleware.state.value == com.mapbox.navigation.mapgpt.core.wakeword.WakeWordState.Connected) {
                middleware.startListening()
            }
        } else {
            middleware.stopListening()
        }
    }
}
