package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public interface NavigationMetricListener {

  void onRouteProgressUpdate(RouteProgress routeProgress);

  void onOffRouteEvent(Location offRouteLocation);

  void onArrival(RouteProgress routeProgress);
}
