package com.mapbox.navigation.dropin.testutil

import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.Middleware
import com.mapbox.navigation.ui.app.internal.State

class RecordDispatchedActions(
    val actions: MutableList<Action> = mutableListOf()
) : Middleware {

    override fun onDispatch(state: State, action: Action): Boolean {
        actions.add(action)
        return false
    }
}
