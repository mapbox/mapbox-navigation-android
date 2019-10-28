package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationDepartEvent(
    phoneState: PhoneState
) : NavigationEvent(phoneState) {

    override fun getEventName(): String = NavigationMetrics.DEPART

    override fun toJson(gson: Gson): String = gson.toJson(this)
}
