package com.mapbox.navigation.dropin.util

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class TestStore : Store() {

    fun setState(state: State) {
        _state.value = state
    }
}
