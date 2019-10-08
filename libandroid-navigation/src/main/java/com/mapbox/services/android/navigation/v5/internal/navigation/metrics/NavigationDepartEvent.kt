package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.annotation.SuppressLint

@SuppressLint("ParcelCreator")
internal class NavigationDepartEvent(
    phoneState: PhoneState
) : NavigationEvent(phoneState) {

    companion object {
        private const val NAVIGATION_DEPART = "navigation.depart"
    }

    override fun getEventName(): String {
        return NAVIGATION_DEPART
    }
}
