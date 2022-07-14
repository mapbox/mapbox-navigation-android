package com.mapbox.navigation.ui.app.testing

import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.Middleware
import com.mapbox.navigation.ui.app.internal.State

/**
 * Middleware that records all dispatched actions.
 */
class TestMiddleware : Middleware {
    val actions = mutableListOf<Action>()

    override fun onDispatch(state: State, action: Action): Boolean {
        actions.add(action)
        return false
    }
}
