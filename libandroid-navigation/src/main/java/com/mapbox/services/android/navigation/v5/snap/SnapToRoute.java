package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

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
    return buildSnappedLocation(location);
  }

  private Location buildSnappedLocation(Location location) {
    Date locationDate = new Date(location.getTime());
    NavigationStatus status = navigator.getStatus(locationDate);
    Location snappedLocation = new Location(location);
    snappedLocation.setLatitude(status.getLocation().latitude());
    snappedLocation.setLongitude(status.getLocation().longitude());
    snappedLocation.setBearing(status.getBearing());
    return snappedLocation;
  }
}