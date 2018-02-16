package com.mapbox.services.android.navigation.ui.v5.route;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class OffRouteEvent {

  private Point newOrigin;
  private RouteProgress routeProgress;

  public OffRouteEvent(Point newOrigin, RouteProgress routeProgress) {
    this.newOrigin = newOrigin;
    this.routeProgress = routeProgress;
  }

  public Point getNewOrigin() {
    return newOrigin;
  }

  public RouteProgress getRouteProgress() {
    return routeProgress;
  }

  public static boolean isValid(OffRouteEvent offRouteEvent) {
    return offRouteEvent.getNewOrigin() != null && offRouteEvent.getRouteProgress() != null;
  }
}
