package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public interface NavigationMetricListeners {

  interface EventListeners {

    void onRouteProgressUpdate(RouteProgress routeProgress);

    void onOffRouteEvent(Location offRouteLocation);
  }

  interface ArrivalListener {

    void onArrival(Location location, RouteProgress routeProgress);
  }
}
