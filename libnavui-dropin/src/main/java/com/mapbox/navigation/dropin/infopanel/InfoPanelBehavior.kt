package com.mapbox.navigation.dropin.infopanel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class InfoPanelBehavior {

    private val _bottomSheetState = MutableStateFlow<Int?>(null)
    val bottomSheetState = _bottomSheetState.asStateFlow()

    private val _slideOffset = MutableStateFlow<Float>(-1f)
    val slideOffset = _slideOffset.asStateFlow()

    fun updateBottomSheetState(newState: Int) {
        _bottomSheetState.value = newState
    }

    fun updateSlideOffset(slideOffset: Float) {
        if (!slideOffset.isNaN()) {
            _slideOffset.value = slideOffset.coerceIn(-1f, 1f)
        }
    }
}
