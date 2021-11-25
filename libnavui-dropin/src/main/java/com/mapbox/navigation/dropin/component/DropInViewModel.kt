package com.mapbox.navigation.dropin.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class DropInViewModel<State, Action>(initialState: State) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    /**
     * The mutex is necessary to prevent race conditions between state mutations.
     *
     * Action processing can be suspended on many steps which can lead to result which is finally delivered being already outdated.
     * Additionally, that might result in actions being interpreted in the wrong order leading to invalid state.
     *
     * Using a mutex ensures that actions are sequenced, regardless of how much time the processing might take.
     *
     * TODO: consider adding actions that are to be evaluated immediately and should dismiss ongoing updates
     */
    private val mutex = Mutex()

    fun consumeAction(action: Flow<Action>) {
        viewModelScope.launch {
            mutex.withLock {
                action.collect {
                    val currentState = state.value
                    val result = process(currentState, action.first())
                    check(
                        _state.compareAndSet(
                            expect = currentState,
                            update = result
                        )
                    ) {
                        """
                            Processing result is for outdated state.
                            There's a race condition.
                        """.trimIndent()
                    }
                }
            }
        }
    }

    abstract suspend fun process(accumulator: State, value: Action): State
}
