package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

public interface RouteListener {
  boolean allowRerouteFrom(Location location);

  void onRerouteFrom(Location location);

  void onRerouteAlong(DirectionsRoute directionsRoute);
}
