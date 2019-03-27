package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.content.Context;

import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation;
import com.mapbox.services.android.navigation.v5.navigation.metrics.SessionState;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

class DepartEventHandler {

  private final Context applicationContext;

  DepartEventHandler(Context applicationContext) {
    this.applicationContext = applicationContext;
  }

  void send(SessionState sessionState, MetricsRouteProgress routeProgress, MetricsLocation location) {
    NavigationMetricsWrapper.departEvent(sessionState, routeProgress, location.getLocation(), applicationContext);
  }
}