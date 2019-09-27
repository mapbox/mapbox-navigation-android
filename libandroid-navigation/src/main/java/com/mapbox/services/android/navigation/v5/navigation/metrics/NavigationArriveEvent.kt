package com.mapbox.services.android.navigation.v5.navigation.metrics

import android.annotation.SuppressLint

private val NAVIGATION_ARRIVE = "navigation.arrive"

@SuppressLint("ParcelCreator")
class NavigationArriveEvent @JvmOverloads constructor(phoneState: PhoneState, override val eventName: String = NAVIGATION_ARRIVE) : NavigationEvent(phoneState) {
    fun eventName() = NAVIGATION_ARRIVE

}
