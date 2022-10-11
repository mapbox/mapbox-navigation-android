package com.mapbox.navigation.ui.app.testing

import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store

internal class TestStore : Store() {

    fun setState(state: State) {
        _state.value = state
    }

    fun updateState(update: (State) -> State) {
        setState(update(_state.value))
    }
}
