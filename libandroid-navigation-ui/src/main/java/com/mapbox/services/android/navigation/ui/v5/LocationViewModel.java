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

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.services.android.navigation.v5.mock.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;

public class LocationViewModel extends AndroidViewModel implements LifecycleObserver, LocationEngineListener {

  final MutableLiveData<LocationEngine> locationEngine = new MutableLiveData<>();
  final MutableLiveData<Location> rawLocation = new MutableLiveData<>();
  private LocationEngine modelLocationEngine;
  private SharedPreferences preferences;

  public LocationViewModel(Application application) {
    super(application);
    preferences = PreferenceManager.getDefaultSharedPreferences(application);
    initLocation(application);
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
  public void onCreate() {
    if (locationEngine.getValue() != null) {
      locationEngine.getValue().addLocationEngineListener(this);
      locationEngine.getValue().activate();
    }
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
    if (modelLocationEngine != null) {
      modelLocationEngine.requestLocationUpdates();
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

  /**
   * Activates the {@link MockLocationEngine} with the
   * give {@link DirectionsRoute}.
   *
   * @param route to be mocked
   */
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
      modelLocationEngine = new LocationSource(application.getApplicationContext());
      modelLocationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
      modelLocationEngine.setFastestInterval(1000);
      modelLocationEngine.setInterval(0);
      this.locationEngine.setValue(modelLocationEngine);

      if (modelLocationEngine.getLastLocation() != null) {
        onLocationChanged(modelLocationEngine.getLastLocation());
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
