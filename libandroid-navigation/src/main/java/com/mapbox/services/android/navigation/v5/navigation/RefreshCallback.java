package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

public interface RefreshCallback {
  void onRefresh(DirectionsRoute directionsRoute);

  void onError(RefreshError error);
}
