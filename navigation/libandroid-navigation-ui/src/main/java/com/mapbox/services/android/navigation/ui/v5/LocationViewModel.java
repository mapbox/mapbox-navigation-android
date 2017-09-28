package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.location.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

public class LocationViewModel extends AndroidViewModel implements LifecycleObserver, LocationEngineListener {

  MutableLiveData<LocationEngine> locationEngine = new MutableLiveData<>();
  MutableLiveData<Location> rawLocation = new MutableLiveData<>();
  private SharedPreferences preferences;

  public LocationViewModel(Application application) {
    super(application);
    preferences = PreferenceManager.getDefaultSharedPreferences(application);
    initLocation(application);
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void onDestroy() {
    deactivateLocationEngine();
  }

  /**
   * Called after the {@link LocationEngine} is activated.
   * Good to request location updates at this point.
   *
   * @since 0.6.0
   */
  @SuppressWarnings( {"MissingPermission"})
  @Override
  public void onConnected() {
    if (locationEngine.getValue() != null) {
      locationEngine.getValue().requestLocationUpdates();
    }
  }

  /**
   * Fired when the {@link LocationEngine} updates.
   * <p>
   * This activity will check launch data here (if we didn't have a location when the map was ready).
   * Once the first location update is received, a new route can be retrieved from {@link NavigationRoute}.
   *
   * @param location used to retrieve route with bearing
   */
  @Override
  public void onLocationChanged(Location location) {
    rawLocation.setValue(location);
  }

  void updateRoute(DirectionsRoute route) {
    if (shouldSimulateRoute()) {
      activateMockLocationEngine(route);
    }
  }

  /**
   * Initializes the {@link LocationEngine} based on whether or not
   * simulation is enabled.
   */
  @SuppressWarnings( {"MissingPermission"})
  private void initLocation(Application application) {
    if (!shouldSimulateRoute()) {
      LocationEngine locationEngine = new LostLocationEngine(application.getApplicationContext());
      locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
      locationEngine.addLocationEngineListener(this);
      locationEngine.setFastestInterval(1000);
      locationEngine.setInterval(0);
      locationEngine.activate();
      this.locationEngine.setValue(locationEngine);

      if (locationEngine.getLastLocation() != null) {
        onLocationChanged(locationEngine.getLastLocation());
      }
    } else {
      // Fire a null location update to fetch the route if we are launching with coordinates
      onLocationChanged(null);
    }
  }

  /**
   * Checks if the route should be simualted with a {@link MockLocationEngine}.
   *
   * @return true if simulation enabled, false if not
   */
  private boolean shouldSimulateRoute() {
    return preferences.getBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, false);
  }

  /**
   * Activates a new {@link MockLocationEngine} with the given
   * {@link DirectionsRoute}.
   *
   * @param route to be mocked
   */
  private void activateMockLocationEngine(DirectionsRoute route) {
    LocationEngine locationEngine = new MockLocationEngine(1000, 30, false);
    ((MockLocationEngine) locationEngine).setRoute(route);
    locationEngine.activate();
    this.locationEngine.setValue(locationEngine);
  }

  /**
   * Deactivates and removes listeners
   * for the {@link LocationEngine} if not null
   */
  private void deactivateLocationEngine() {
    if (locationEngine.getValue() != null) {
      locationEngine.getValue().removeLocationUpdates();
      locationEngine.getValue().removeLocationEngineListener(this);
      locationEngine.getValue().deactivate();
    }
  }
}
