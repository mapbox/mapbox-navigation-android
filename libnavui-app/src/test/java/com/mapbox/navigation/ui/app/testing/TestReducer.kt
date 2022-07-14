package com.mapbox.navigation.ui.app.testing

import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.Reducer
import com.mapbox.navigation.ui.app.internal.State

/**
 * Reducer that records all dispatched actions.
 */
internal class TestReducer : Reducer {
    val actions = mutableListOf<Action>()

    override fun process(state: State, action: Action): State {
        actions.add(action)
        return state
    }
}
