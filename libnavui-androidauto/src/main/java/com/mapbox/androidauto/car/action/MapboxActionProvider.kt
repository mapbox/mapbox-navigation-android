package com.mapbox.androidauto.car.action

import androidx.car.app.Screen
import androidx.car.app.model.Action

fun interface MapboxActionProvider {
    fun getAction(screen: Screen): Action
}
