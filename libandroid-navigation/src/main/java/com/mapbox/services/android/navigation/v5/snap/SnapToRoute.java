package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class SnapToRoute extends Snap {

  @Override
  public Location getSnappedLocation(Location location, RouteProgress routeProgress) {
    // No impl
    return location;
  }

  public Location getSnappedLocationWith(NavigationStatus status, Location rawLocation) {
    return buildSnappedLocation(status, rawLocation);
  }

  @NonNull
  private Location buildSnappedLocation(NavigationStatus status, Location rawLocation) {
    Location snappedLocation = new Location(rawLocation);
    FixLocation fixLocation = status.getLocation();
    Point coordinate = fixLocation.getCoordinate();
    snappedLocation.setLatitude(coordinate.latitude());
    snappedLocation.setLongitude(coordinate.longitude());
    if (fixLocation.getBearing() != null) {
      snappedLocation.setBearing(fixLocation.getBearing());
    }
    snappedLocation.setTime(fixLocation.getTime().getTime());
    return snappedLocation;
  }
}