package com.mapbox.navigation.ui.app.internal.extension

import com.mapbox.navigation.ui.app.internal.Store

/**
 * Interface that describes "thunk" actions.
 */
fun interface ThunkAction {
    /**
     * Thunk actions accept [Store] reference that can be used to access current [Store.state] and
     * [Store.dispatch] for notifying of a computation result.
     */
    operator fun invoke(store: Store)
}

/**
 * Extension that allows dispatching ThunkActions.
 */
fun Store.dispatch(thunkAction: ThunkAction) {
    thunkAction(this)
}
