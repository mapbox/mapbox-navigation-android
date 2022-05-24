package com.mapbox.navigation.dropin.model

import com.mapbox.navigation.ui.utils.internal.extensions.slice
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Marks a class as an action that can be dispatched with [Store].
 */
interface Action

/**
 * Implementation of this interface can be registered with the [Store]
 * to process dispatched [Action]'s and update store [State].
 */
internal fun interface Reducer {

    /**
     * The callback invoked for each dispatched [Action].
     *
     * @param state current [State]
     * @param action dispatched [Action]
     * @return updated store [State]
     */
    fun process(state: State, action: Action): State
}

/**
 * A store that holds observable, drop-in UI [State]. The [State] can only be modified by
 * registered [Reducer], as a result of [Action] processing.
 */
internal open class Store {
    protected val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val reducers = ConcurrentLinkedQueue<Reducer>()
    private var isDispatching = false

    fun <T> select(selector: (State) -> T): Flow<T> {
        return state.map { selector(it) }.distinctUntilChanged()
    }

    fun <T> slice(scope: CoroutineScope, selector: (State) -> T): StateFlow<T> {
        return state.slice(scope, selector = selector)
    }

    fun dispatch(action: Action) {
        if (isDispatching) {
            logW(
                "Cannot dispatch new actions during reducer processing. " +
                    "Action dropped: ${action::class.java.simpleName}.",
                "Store"
            )
            return
        }

        isDispatching = true
        reducers.forEach { reducer ->
            _state.value = reducer.process(_state.value, action)
        }
        isDispatching = false
    }

    fun register(vararg reducers: Reducer) {
        this.reducers.addAll(reducers)
    }

    fun unregister(vararg reducers: Reducer) {
        this.reducers.removeAll(reducers)
    }
}
