package com.mapbox.navigation.dropin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.dropin.ViewStateTransitionProcessor.TransitionToEmpty.process
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

@ExperimentalCoroutinesApi
internal class NavigationViewModel : ViewModel() {

    private val _actions: MutableStateFlow<NavigationStateTransitionAction?> =
        MutableStateFlow(null)
    private val _viewStates: MutableStateFlow<NavigationViewState> =
        MutableStateFlow(NavigationViewState.UponEmpty())

    init {
        _actions
            .filterNotNull()
            .toProcessor()
            .process()
            .toState()
            .onEach { _viewStates.value = it }
            .launchIn(viewModelScope)
    }

    fun processAction(action: NavigationStateTransitionAction) {
        _actions.value = action
    }

    fun viewStates(): Flow<NavigationViewState> = _viewStates

    private fun Flow<NavigationStateTransitionAction>.toProcessor(): Flow<ViewStateTransitionProcessor> =
        map { action ->
            when (action) {
                is NavigationStateTransitionAction.ToEmpty -> ViewStateTransitionProcessor.TransitionToEmpty
                is NavigationStateTransitionAction.ToFreeDrive -> ViewStateTransitionProcessor.TransitionToFreeDrive
                is NavigationStateTransitionAction.ToRoutePreview -> ViewStateTransitionProcessor.TransitionToRoutePreview
                is NavigationStateTransitionAction.ToActiveNavigation -> ViewStateTransitionProcessor.TransitionToActiveNavigation
                is NavigationStateTransitionAction.ToArrival -> ViewStateTransitionProcessor.TransitionToArrival
            }
        }

    private fun Flow<NavigationStateTransitionResult>.toState(): Flow<NavigationViewState> =
        scan(NavigationViewState.UponEmpty(NavigationState.Empty)) { accumulator, result ->
            when (result) {
                is NavigationStateTransitionResult.ToEmpty -> accumulator.copy(navigationState = result.state)
                is NavigationStateTransitionResult.ToFreeDrive -> accumulator.copy(navigationState = result.state)
                is NavigationStateTransitionResult.ToRoutePreview -> accumulator.copy(
                    navigationState = result.state
                )
                is NavigationStateTransitionResult.ToActiveNavigation -> accumulator.copy(
                    navigationState = result.state
                )
                is NavigationStateTransitionResult.ToArrival -> accumulator.copy(navigationState = result.state)
            }
        }
}
