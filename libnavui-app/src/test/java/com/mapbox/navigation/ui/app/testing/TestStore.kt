package com.mapbox.navigation.ui.app.testing

import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.Middleware
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store

internal class TestStore : Store() {

    val actions = mutableListOf<Action>()

    init {
        registerMiddleware(object : Middleware {
            override fun onDispatch(state: State, action: Action): Boolean {
                actions.add(action)
                return false
            }
        })
    }

    fun setState(state: State) {
        _state.value = state
    }

    fun updateState(update: (State) -> State) {
        setState(update(_state.value))
    }

    fun <T : Action> didDispatchAction(action: T) = actions.contains(action)
}
