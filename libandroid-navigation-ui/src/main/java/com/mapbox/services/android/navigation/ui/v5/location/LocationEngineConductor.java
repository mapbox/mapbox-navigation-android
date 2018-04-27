package com.mapbox.services.android.navigation.ui.v5.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;

public class LocationEngineConductor {

  private static final int UPDATE_DELAY_IN_MILLIS = 1000;
  private static final int SPEED_IN_MPH = 30;
  private static final int FASTEST_INTERVAL_IN_MILLIS = 1000;
  private static final int INTERVAL_IN_MILLIS = 0;

  private LocationEngineConductorListener listener;
  private LocationEngine locationEngine;

  public LocationEngineConductor(LocationEngineConductorListener listener) {
    this.listener = listener;
  }

  public void onCreate() {
    activateLocationEngine();
  }

  public void onDestroy() {
    deactivateLocationEngine();
  }

  public void initializeLocationEngine(Context context, boolean simulateRoute) {
    initLocationEngine(context, simulateRoute);
  }

  public void updateRoute(DirectionsRoute route) {
    if (locationEngine instanceof MockLocationEngine) {
      ((MockLocationEngine) locationEngine).setRoute(route);
    }
  }

  public LocationEngine obtainLocationEngine() {
    return locationEngine;
  }

  private void initLocationEngine(Context context, boolean simulateRoute) {
    if (simulateRoute) {
      locationEngine = new MockLocationEngine(UPDATE_DELAY_IN_MILLIS, SPEED_IN_MPH, false);
    } else {
      LocationEngineProvider locationEngineProvider = new LocationEngineProvider(context.getApplicationContext());
      locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
      locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
      locationEngine.setFastestInterval(FASTEST_INTERVAL_IN_MILLIS);
      locationEngine.setInterval(INTERVAL_IN_MILLIS);
      updateLastLocation();
    }
    activateLocationEngine();
  }

  private void activateLocationEngine() {
    if (isValidLocationEngine()) {
      locationEngine.addLocationEngineListener(locationEngineListener);
      locationEngine.activate();
    }
  }

  private void deactivateLocationEngine() {
    if (isValidLocationEngine()) {
      locationEngine.removeLocationUpdates();
      locationEngine.removeLocationEngineListener(locationEngineListener);
      locationEngine.deactivate();
    }
  }

  @SuppressWarnings( {"MissingPermission"})
  private void updateLastLocation() {
    if (locationEngine.getLastLocation() != null) {
      listener.onLocationUpdate(locationEngine.getLastLocation());
    }
  }

  private boolean isValidLocationEngine() {
    return locationEngine != null;
  }

  private LocationEngineListener locationEngineListener = new LocationEngineListener() {
    @SuppressLint("MissingPermission")
    @Override
    public void onConnected() {
      if (isValidLocationEngine()) {
        locationEngine.requestLocationUpdates();
      }
    }

    @Override
    public void onLocationChanged(Location location) {
      listener.onLocationUpdate(location);
    }
  };
}
