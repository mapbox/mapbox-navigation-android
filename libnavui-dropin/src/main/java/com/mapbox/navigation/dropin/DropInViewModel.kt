package com.mapbox.navigation.dropin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.maps.MapboxMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

class DropInViewModel(var options: DropInOptions): ViewModel() {

    val userAction = Channel<Action>(UNLIMITED)
    private lateinit var mapboxMap: MapboxMap

    init {
        observeUserActions()
    }

    private fun observeUserActions() {
        viewModelScope.launch {
            userAction.consumeAsFlow().collect { action ->
                when (action) {
                    is MapInitializationAction.OnMapViewInitialized -> {
                        mapboxMap = action.mapboxMap
                    }
                }
            }
        }
    }
}
