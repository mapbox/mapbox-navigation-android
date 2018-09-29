package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class SnapToRoute extends Snap {

  private static final String NAVIGATOR_SNAPPED_LOCATION = "NavigatorSnappedLocation";

  @Override
  public Location getSnappedLocation(Location location, RouteProgress routeProgress) {
    // No impl
    return location;
  }

  public Location getSnappedLocationWith(NavigationStatus status) {
    return buildSnappedLocation(status);
  }

  @NonNull
  private Location buildSnappedLocation(NavigationStatus status) {
    Location snappedLocation = new Location(NAVIGATOR_SNAPPED_LOCATION);
    snappedLocation.setLatitude(status.getLocation().latitude());
    snappedLocation.setLongitude(status.getLocation().longitude());
    snappedLocation.setBearing(status.getBearing());
    snappedLocation.setTime(status.getTime().getTime());
    return snappedLocation;
  }
}