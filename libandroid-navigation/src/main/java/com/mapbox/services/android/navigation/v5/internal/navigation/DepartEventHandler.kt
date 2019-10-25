package com.mapbox.services.android.navigation.v5.internal.navigation

import android.content.Context
import com.google.gson.Gson
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.metrics.NavigationMetrics
import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationEventFactory
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.PhoneState
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.SessionState
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress

internal class DepartEventHandler(
    private val applicationContext: Context,
    private val gson: Gson,
    private val sdkIdentifier: String,
    private val metricsReporter: MetricsReporter
) {

    fun send(
        sessionState: SessionState,
        routeProgress: MetricsRouteProgress,
        location: MetricsLocation
    ) {
        val event = NavigationEventFactory.buildNavigationDepartEvent(
            PhoneState(applicationContext),
            sessionState,
            routeProgress,
            location.location,
            sdkIdentifier
        )
        metricsReporter.addEvent(NavigationMetrics.DEPART, gson.toJson(event))
    }
}
