package com.mapbox.navigation.dropin.component.navigationstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationStateViewModel : ViewModel() {
    private val _navigationState: MutableStateFlow<NavigationState> =
        MutableStateFlow(NavigationState.Empty)

    internal fun navigationState(): Flow<NavigationState> = _navigationState

    internal suspend fun processAction(action: Flow<NavigationStateAction>) {
        action
            .toProcessor()
            .toResult()
            .reduce()
            .onEach { _navigationState.value = it }
            .stateIn(viewModelScope)
    }

    private fun Flow<NavigationStateResult>.reduce(): Flow<NavigationState> =
        map { result ->
            when (result) {
                is NavigationStateResult.ToEmpty -> {
                    result.navigationState
                }
                is NavigationStateResult.ToFreeDrive -> {
                    result.navigationState
                }
                is NavigationStateResult.ToRoutePreview -> {
                    result.navigationState
                }
                is NavigationStateResult.ToActiveNavigation -> {
                    result.navigationState
                }
                is NavigationStateResult.ToArrival -> {
                    result.navigationState
                }
            }
        }
}
