package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.annotation.SuppressLint

@SuppressLint("ParcelCreator")
internal class NavigationArriveEvent(
    phoneState: PhoneState
) : NavigationEvent(phoneState) {

    companion object {
        private const val NAVIGATION_ARRIVE = "navigation.arrive"
    }

    override fun getEventName(): String = NAVIGATION_ARRIVE
}
