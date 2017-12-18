package com.mapbox.services.android.navigation.ui.v5;

import android.graphics.Point;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

public interface RouteListener {

  boolean allowRerouteFrom(Point offRoutePoint);

  void onOffRoute(Point offRoutePoint);

  void onRerouteAlong(DirectionsRoute directionsRoute);

  void onFailedReroute(String error);
}
