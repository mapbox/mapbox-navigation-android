package com.mapbox.services.android.navigation.ui.v5.location;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.OnLifecycleEvent;
import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.location.LocationEngineProvider;


public class LocationViewModel extends AndroidViewModel implements LifecycleObserver, LocationEngineListener {

  public final MutableLiveData<LocationEngine> locationEngine = new MutableLiveData<>();
  public final MutableLiveData<Location> rawLocation = new MutableLiveData<>();
  private LocationEngine modelLocationEngine;
  private boolean shouldSimulateRoute;

  public LocationViewModel(Application application) {
    super(application);
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
   * Checks {@link com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions} to see if the route
   * should be simulated with a {@link MockLocationEngine}.
   */
  public void updateShouldSimulateRoute(boolean shouldSimulateRoute) {
    this.shouldSimulateRoute = shouldSimulateRoute;
  }

  /**
   * Activates the {@link MockLocationEngine} with the
   * give {@link DirectionsRoute}.
   *
   * @param route to be mocked
   */
  public void updateRoute(DirectionsRoute route) {
    // MockLocationEngine is deactivated first to avoid weird behavior with subsequent navigation sessions
    if (shouldSimulateRoute) {
      deactivateLocationEngine();
      activateMockLocationEngine(route);
    }
  }

  /**
   * Initializes the {@link LocationEngine} based on whether or not
   * simulation is enabled.
   */
  @SuppressWarnings({"MissingPermission"})
  private void initLocation(Application application) {
    if (!shouldSimulateRoute) {
      LocationEngineProvider locationEngineProvider = new LocationEngineProvider(application.getApplicationContext());
      modelLocationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
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
