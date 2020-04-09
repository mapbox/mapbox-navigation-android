package com.mapbox.navigation.ui.junction;

import com.mapbox.navigation.base.trip.model.RouteProgress;

public class RouteJunctionModel {

  private RouteProgress routeProgress;

  public RouteJunctionModel(RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
  }

  public RouteProgress getRouteProgress() {
    return routeProgress;
  }
}
