package com.mapbox.navigation.dropin

import android.view.View

data class ViewProvider(
    val maneuverProvider: (() -> View)? = null,
    val tripProgressProvider: (() -> View)? = null,
    val recenterButtonProvider: (() -> View)? = null,
    val routeOverviewButtonProvider: (() -> View)? = null,
)
