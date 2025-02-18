package com.mapbox.navigation.mapgpt.core.userinput

import com.mapbox.navigation.mapgpt.core.MiddlewareManager
import com.mapbox.navigation.mapgpt.core.MiddlewareSwitcher
import com.mapbox.navigation.mapgpt.core.common.PlatformSettingsFactory
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.common.getString
import com.mapbox.navigation.mapgpt.core.common.setString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Provides an ability to switch between different implementations of [UserInputOwner].
 */
class UserInputMiddlewareManager(
    initialUserInputMiddleware: Set<UserInputOwnerMiddleware>,
) : MiddlewareManager<UserInputMiddlewareContext, UserInputOwnerMiddleware>() {

    private val persistentSettings by lazy {
        PlatformSettingsFactory.createPersistentSettings()
    }

    private val _availableUserInputMiddleware by lazy {
        MutableStateFlow(initialUserInputMiddleware)
    }
    val availableMiddleware: StateFlow<Set<UserInputOwnerMiddleware>> by lazy {
        _availableUserInputMiddleware
    }

    private val _userInputOwnerMiddlewareSwitcher by lazy {
        val initialMiddleware = persistentSettings.getString(USER_INPUT_PROVIDER_KEY)?.let { key ->
            availableMiddleware.value.firstOrNull { it.provider.key == key }
        } ?: initialUserInputMiddleware.first()
        UserInputOwnerMiddlewareSwitcher(initialMiddleware)
    }
    override val middlewareSwitchers:
        Set<MiddlewareSwitcher<UserInputMiddlewareContext, UserInputOwnerMiddleware>> by lazy {
            setOf(_userInputOwnerMiddlewareSwitcher)
        }

    /**
     * The middleware interface that allows you to interact [UserInputOwner].
     */
    val userInputOwner: UserInputOwner by lazy { _userInputOwnerMiddlewareSwitcher }
    val userInputMiddleware: StateFlow<UserInputOwnerMiddleware> by lazy {
        _userInputOwnerMiddlewareSwitcher.middlewareState
    }

    override fun onAttached(middlewareContext: UserInputMiddlewareContext) {
        super.onAttached(middlewareContext)
        _userInputOwnerMiddlewareSwitcher.middlewareState.onEach {
            persistentSettings.setString(key = USER_INPUT_PROVIDER_KEY, value = it.provider.key)
        }.launchIn(ioScope)

        // Capture errors and logs from middleware state
        userInputOwner.state.onEach { state ->
            val providerKey = userInputMiddleware.value.provider.key
            if (state is UserInputState.Error) {
                SharedLog.e(TAG) { "$providerKey error: ${state.reason}"}
            } else {
                SharedLog.d(TAG) { "$providerKey state: $state"}
            }
        }.launchIn(mainScope)
    }

    /**
     * Set the default Speech to Text middleware.
     */
    fun setDefaultUserInputOwnerMiddleware() {
        _userInputOwnerMiddlewareSwitcher.unregisterMiddleware()
    }

    /**
     * Set the Text to Speech middleware.
     * There can only be one [UserInputOwnerMiddleware] at a time.
     */
    fun setUserInputOwnerMiddleware(middleware: UserInputOwnerMiddleware) {
        _userInputOwnerMiddlewareSwitcher.registerMiddleware(middleware)
    }

    private companion object {
        private const val TAG = "UserInputMiddlewareManager"
        private const val USER_INPUT_PROVIDER_KEY =
            "mapbox_dash_user_input_provider_key"
    }
}
