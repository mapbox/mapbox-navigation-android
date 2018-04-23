package com.mapbox.services.android.navigation.ui.v5.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.location.LocationEngineProvider;

public class NavigationLocationEngine {

  private NavigationLocationEngineCallback locationEngineCallback;
  private LocationEngine locationEngine;

  public NavigationLocationEngine(NavigationLocationEngineCallback locationEngineCallback) {
    this.locationEngineCallback = locationEngineCallback;
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
    LocationEngineProvider locationEngineProvider = new LocationEngineProvider(context.getApplicationContext());
    if (simulateRoute) {
      locationEngine = new MockLocationEngine(1000, 30, false);
    } else {
      locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
      locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
      locationEngine.setFastestInterval(1000);
      locationEngine.setInterval(0);
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
      locationEngineCallback.onLocationUpdate(locationEngine.getLastLocation());
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
      locationEngineCallback.onLocationUpdate(location);
    }
  };
}
