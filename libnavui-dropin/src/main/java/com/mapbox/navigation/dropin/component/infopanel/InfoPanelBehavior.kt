package com.mapbox.navigation.dropin.component.infopanel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class InfoPanelBehavior {

    private val _infoPanelBehavior = MutableStateFlow<Int?>(null)
    val infoPanelBehavior = _infoPanelBehavior.asStateFlow()

    fun updateBehavior(newState: Int) {
        _infoPanelBehavior.value = newState
    }
}
