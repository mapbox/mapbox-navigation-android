package com.mapbox.services.android.navigation.ui.v5.location;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;

import timber.log.Timber;

class NavigationViewLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

  private final LocationEngineConductorListener listener;

  NavigationViewLocationEngineCallback(LocationEngineConductorListener listener) {
    this.listener = listener;
  }

  @Override
  public void onSuccess(LocationEngineResult result) {
    Location location = result.getLastLocation();
    listener.onLocationUpdate(location);
  }

  @Override
  public void onFailure(@NonNull Exception exception) {
    Timber.e(exception);
  }
}
