package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

public interface OfflineRouteFoundCallback {
  void onOfflineRouteFound(DirectionsRoute directionsRoute);
}
