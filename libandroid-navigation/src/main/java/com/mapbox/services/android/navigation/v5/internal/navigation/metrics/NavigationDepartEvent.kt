package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.annotation.SuppressLint
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationDepartEvent(
    phoneState: PhoneState
) : NavigationEvent(phoneState) {

    override fun getEventName(): String = NavigationMetrics.DEPART
}
