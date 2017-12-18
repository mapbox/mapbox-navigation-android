package com.mapbox.services.android.navigation.ui.v5;


import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

public interface RouteListener {

  boolean allowRerouteFrom(Point offRoutePoint);

  void onOffRoute(Point offRoutePoint);

  void onRerouteAlong(DirectionsRoute directionsRoute);

  void onFailedReroute();
}
