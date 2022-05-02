package com.mapbox.androidauto.car.action

import androidx.car.app.Screen
import androidx.car.app.model.Action

sealed interface MapboxActionProvider {
    interface ActionProvider : MapboxActionProvider {
        fun getAction(): Action
    }
    interface ScreenActionProvider : MapboxActionProvider {
        fun getAction(screen: Screen): Action
    }
}
