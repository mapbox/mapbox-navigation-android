package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.Navigator;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.Date;

public class SnapToRoute extends Snap {

  private final Navigator navigator;

  public SnapToRoute(Navigator navigator) {
    this.navigator = navigator;
  }

  @Override
  public Location getSnappedLocation(Location location, RouteProgress routeProgress) {
    // No impl
    return location;
  }

  public Location getSnappedLocationWith(Location location, Date date) {
    return buildSnappedLocation(location, date);
  }

  @NonNull
  private Location buildSnappedLocation(Location location, Date date) {
    NavigationStatus status = navigator.getStatus(date);
    Location snappedLocation = new Location(location);
    snappedLocation.setLatitude(status.getLocation().latitude());
    snappedLocation.setLongitude(status.getLocation().longitude());
    snappedLocation.setBearing(status.getBearing());
    return snappedLocation;
  }
}