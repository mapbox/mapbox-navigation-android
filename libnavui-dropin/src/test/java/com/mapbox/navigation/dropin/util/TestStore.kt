package com.mapbox.navigation.dropin.util

import com.mapbox.navigation.dropin.testutil.RecordDispatchedActions
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store

internal class TestStore : Store() {
    val actions = mutableListOf<Action>()

    init {
        registerMiddleware(RecordDispatchedActions(actions))
    }

    fun setState(state: State) {
        _state.value = state
    }

    fun updateState(update: (State) -> State) {
        setState(update(_state.value))
    }
}
