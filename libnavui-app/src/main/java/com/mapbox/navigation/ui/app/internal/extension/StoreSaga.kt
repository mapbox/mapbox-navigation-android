package com.mapbox.navigation.ui.app.internal.extension

import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.Middleware
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Delays coroutine until an [Action] that matches the [predicate] has been dispatched
 * to the receiving [Store].
 */
suspend fun Store.takeAction(predicate: (action: Action) -> Boolean): Action =
    suspendCancellableCoroutine { cont ->
        val m = object : Middleware {
            override fun onDispatch(state: State, action: Action): Boolean {
                if (predicate(action)) {
                    cont.resume(action)
                    unregisterMiddleware(this)
                }
                return false
            }
        }
        registerMiddleware(m)
        cont.invokeOnCancellation { unregisterMiddleware(m) }
    }

/**
 * Delays coroutine until an [Action] of a type T has been dispatched to the receiving [Store].
 *
 * It is a shorthand for `store.takeAction { it is MyAction }`.
 */
suspend inline fun <reified T : Action> Store.takeAction(): T =
    takeAction { it is T } as T

/**
 * Create an instance of a cold Flow with all Actions dispatched to the receiving Store.
 *
 * The resulting flow is cold, which means it will contain only dispatched actions since
 * a terminal operator was applied to the returned flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Store.actionsFlowable(): Flow<Action> = callbackFlow {
    val m = object : Middleware {
        override fun onDispatch(state: State, action: Action): Boolean {
            trySend(action)
            return false
        }
    }
    registerMiddleware(m)
    awaitClose { unregisterMiddleware(m) }
}

/**
 * Terminal flow operator that collects only latest Action of type [T] from the receiving Flow.
 *
 * Just like [Flow.collectLatest], the action block for the previous value is cancelled
 * when the original flow emits a new value.
 *
 * It is a shorthand for `flow.filterIsInstance<MyAction>().collectLatest { block(it) }`.
 */
suspend inline fun <reified T : Action> Flow<Action>.collectLatestAction(
    crossinline block: suspend (T) -> Unit
) {
    filterIsInstance<T>().collectLatest { block(it) }
}
