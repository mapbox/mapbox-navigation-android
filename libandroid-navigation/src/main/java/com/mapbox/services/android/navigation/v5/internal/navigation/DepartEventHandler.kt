package com.mapbox.services.android.navigation.v5.internal.navigation

import android.content.Context
import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.SessionState
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress

internal class DepartEventHandler(private val applicationContext: Context) {

    fun send(
        sessionState: SessionState,
        routeProgress: MetricsRouteProgress,
        location: MetricsLocation
    ) {
        NavigationMetricsWrapper.departEvent(
            sessionState,
            routeProgress,
            location.location,
            applicationContext
        )
    }
}
