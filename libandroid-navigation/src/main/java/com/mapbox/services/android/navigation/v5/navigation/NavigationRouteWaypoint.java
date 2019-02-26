package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.geojson.Point;

class NavigationRouteWaypoint {

  private final Point waypoint;
  private final Double bearingAngle;
  private final Double tolerance;

  NavigationRouteWaypoint(@NonNull Point waypoint, @Nullable Double bearingAngle,
                          @Nullable Double tolerance) {
    this.waypoint = waypoint;
    this.bearingAngle = bearingAngle;
    this.tolerance = tolerance;
  }

  @NonNull
  Point getWaypoint() {
    return waypoint;
  }

  @Nullable
  Double getBearingAngle() {
    return bearingAngle;
  }

  @Nullable
  Double getTolerance() {
    return tolerance;
  }
}
