package com.mapbox.navigation.dropin.lifecycle

import androidx.annotation.CallSuper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implement your version of a behavior. Behaviors do not reference android ui elements directly,
 * this is because their lifecycle will survive beyond a view or activity.
 *
 * UIViewModels have a lifecycle, contain state, and process actions.
 *
 * @param initialState used to initialize the [state]
 * @property action
 * @property state
 * @property mainJobControl
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class UIViewModel<State, Action>(initialState: State) : MapboxNavigationObserver {

    // TODO Potential mechanism to expose actions in the sdk through a public api
    private val _action = MutableSharedFlow<Action>(extraBufferCapacity = 1)
    val action: Flow<Action> = _action

    private val _state: MutableStateFlow<State> = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state

    lateinit var mainJobControl: JobControl

    /**
     * Invoke an action for the behavior.
     */
    fun invoke(action: Action): State {
        _action.tryEmit(action)
        val mapboxNavigation: MapboxNavigation? = MapboxNavigationApp.current()
        return if (mapboxNavigation != null) {
            process(mapboxNavigation, _state.value, action).also {
                _state.value = it
            }
        } else {
            logW(
                "Cannot invoke action when MapboxNavigationApp is not setup $action",
                LOG_CATEGORY
            )
            _state.value
        }
    }

    /**
     * When you create a behavior, implement how the behavior should process actions and update
     * state. If there is no state associated with the behavior, use Unit.
     */
    abstract fun process(mapboxNavigation: MapboxNavigation, state: State, action: Action): State

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     * @param mapboxNavigation
     */
    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mainJobControl = InternalJobControlFactory.createMainScopeJobControl()
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     * @param mapboxNavigation
     */
    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mainJobControl.job.cancelChildren()
    }

    private companion object {
        private val LOG_CATEGORY = UIViewModel::class.java.simpleName
    }
}
