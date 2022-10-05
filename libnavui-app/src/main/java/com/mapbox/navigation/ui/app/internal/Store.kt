package com.mapbox.navigation.ui.app.internal

import com.mapbox.navigation.ui.utils.internal.extensions.slice
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
fun interface Reducer {

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
 * Implementation of this interface can be registered with the [Store]
 * to intercept dispatched [Action]'s.
 */
interface Middleware {
    /**
     * The callback invoked for each dispatched [Action].
     *
     * @param state current [State]
     * @param action dispatched [Action]
     * @return `true` if [action] was consumed by this middleware and should not be processed by the Store.
     */
    fun onDispatch(state: State, action: Action): Boolean
}

/**
 * A store that holds observable, drop-in UI [State]. The [State] can only be modified by
 * registered [Reducer], as a result of [Action] processing.
 */
open class Store {
    protected val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val middlewares = ConcurrentLinkedQueue<Middleware>()
    private val reducers = ConcurrentLinkedQueue<Reducer>()
    private val actions = ConcurrentLinkedQueue<Action>()
    private var isProcessing = false

    fun <T> select(selector: (State) -> T): Flow<T> {
        return state.map { selector(it) }.distinctUntilChanged()
    }

    fun <T> slice(scope: CoroutineScope, selector: (State) -> T): StateFlow<T> {
        return state.slice(scope, selector = selector)
    }

    fun dispatch(action: Action) {
        actions.add(action)
        if (isProcessing) return

        isProcessing = true
        while (actions.isNotEmpty()) {
            val head = actions.remove()
            if (!intercept(head)) {
                reduce(head)
            }
        }
        isProcessing = false
    }

    private fun intercept(action: Action): Boolean {
        middlewares.forEach {
            if (it.onDispatch(_state.value, action)) return true
        }
        return false
    }

    private fun reduce(action: Action) {
        reducers.forEach { reducer ->
            _state.value = reducer.process(_state.value, action)
        }
    }

    fun register(vararg reducers: Reducer) {
        this.reducers.addAll(reducers)
    }

    fun unregister(vararg reducers: Reducer) {
        this.reducers.removeAll(reducers)
    }

    fun registerMiddleware(vararg middlewares: Middleware) {
        this.middlewares.addAll(middlewares)
    }

    fun unregisterMiddleware(vararg middlewares: Middleware) {
        this.middlewares.removeAll(middlewares)
    }

    /**
     * Resets [state] back to the initial state.
     */
    fun reset() {
        _state.value = State()
    }
}
