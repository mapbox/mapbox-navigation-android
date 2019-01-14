package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;

class ReplayRouteLocationListener implements ReplayLocationListener {

  private final ReplayRouteLocationEngine engine;
  private final LocationEngineCallback<LocationEngineResult> callback;

  ReplayRouteLocationListener(ReplayRouteLocationEngine engine,
                              LocationEngineCallback<LocationEngineResult> callback) {
    this.engine = engine;
    this.callback = callback;
  }

  @Override
  public void onLocationReplay(Location location) {
    engine.updateLastLocation(location);
    engine.removeLastMockedLocation();
    LocationEngineResult result = LocationEngineResult.create(location);
    callback.onSuccess(result);
  }
}
