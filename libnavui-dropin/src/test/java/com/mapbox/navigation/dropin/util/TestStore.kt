package com.mapbox.navigation.dropin.util

import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.model.Store

internal class TestStore : Store() {

    fun setState(state: State) {
        _state.value = state
    }
}
