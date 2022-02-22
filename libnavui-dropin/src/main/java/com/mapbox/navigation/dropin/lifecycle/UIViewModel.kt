package com.mapbox.navigation.dropin.lifecycle

import androidx.annotation.CallSuper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Implement your version of a behavior. Behaviors do not reference android ui elements directly,
 * this is because their lifecycle will survive beyond a view or activity.
 *
 * Behaviors have a lifecycle, contain state, and process actions. [UIComponent] will respond
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
abstract class UIViewModel<State, Action>(
    mutableState: MutableStateFlow<State>
) : MapboxNavigationObserver {

    constructor(initialState: State) : this(MutableStateFlow(initialState))

    private val _action = MutableSharedFlow<Action>(extraBufferCapacity = 1)
    val action: Flow<Action> = _action
    private val _state: MutableStateFlow<State> = mutableState
    val state: StateFlow<State> = _state

    lateinit var mainJobControl: JobControl

    /**
     * Invoke an action for the behavior.
     */
    fun invoke(action: Action) {
        _action.tryEmit(action)
    }

    /**
     * When you create a behavior, implement how the behavior should process actions and update
     * state. If there is no state associated with the behavior, use Unit.
     */
    abstract fun process(mapboxNavigation: MapboxNavigation, state: State, action: Action): State

    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mainJobControl = InternalJobControlFactory.createMainScopeJobControl()

        mainJobControl.scope.launch {
            action.collect {
                _state.value = process(mapboxNavigation, _state.value, it)
            }
        }
    }

    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mainJobControl.job.cancelChildren()
    }
}
