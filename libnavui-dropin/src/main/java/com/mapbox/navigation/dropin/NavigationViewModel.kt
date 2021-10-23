package com.mapbox.navigation.dropin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

internal class NavigationViewModel: ViewModel() {

    val handleAction = Channel<Action>(UNLIMITED)
    private val _viewState = MutableStateFlow<NavigationViewState>(NavigationViewState.UponEmpty())
    val viewState: StateFlow<NavigationViewState>
        get() = _viewState
    val reducer: (NavigationViewState, Result) -> Unit = { _, result ->
        when (result) {
            is NavigationStateTransitionResult.ToEmpty -> {
                _viewState.value = result.state
            }
            is NavigationStateTransitionResult.ToFreeDrive -> {
                _viewState.value = result.state
            }
            is NavigationStateTransitionResult.ToRoutePreview -> {
                _viewState.value = result.state
            }
            is NavigationStateTransitionResult.ToActiveNavigation -> {
                _viewState.value = result.state
            }
            is NavigationStateTransitionResult.ToArrival -> {
                _viewState.value = result.state
            }
        }
    }

    init {
        observeUserActions()
    }

    private fun observeUserActions() {
        viewModelScope.launch {
            handleAction.consumeAsFlow().collect { action ->
                when (action) {
                    is NavigationStateTransitionAction.ToEmpty -> {
                        reducer(_viewState.value, processTransition(action))
                    }
                }
            }
        }
    }
}
