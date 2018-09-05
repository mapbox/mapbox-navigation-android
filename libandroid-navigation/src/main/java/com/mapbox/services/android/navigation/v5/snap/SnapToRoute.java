package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class SnapToRoute extends Snap {

  @Override
  public Location getSnappedLocation(Location location, RouteProgress routeProgress) {
    // No impl
    return location;
  }

  public Location getSnappedLocationWith(Location location, NavigationStatus status) {
    return buildSnappedLocation(location, status);
  }

  @NonNull
  private Location buildSnappedLocation(Location location, NavigationStatus status) {
    Location snappedLocation = new Location(location);
    snappedLocation.setLatitude(status.getLocation().latitude());
    snappedLocation.setLongitude(status.getLocation().longitude());
    snappedLocation.setBearing(status.getBearing());
    return snappedLocation;
  }
}