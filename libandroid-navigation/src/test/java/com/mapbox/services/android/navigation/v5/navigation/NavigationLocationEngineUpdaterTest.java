package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationLocationEngineUpdaterTest {

  @Test
  public void onInitialization_engineListenerIsSet() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    NavigationLocationEngineListener listener = mock(NavigationLocationEngineListener.class);

    new NavigationLocationEngineUpdater(locationEngine, listener);

    verify(locationEngine).addLocationEngineListener(eq(listener));
  }

  @Test
  public void updateLocationEngine_engineListenerIsAdded() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    NavigationLocationEngineListener listener = mock(NavigationLocationEngineListener.class);
    NavigationLocationEngineUpdater provider = new NavigationLocationEngineUpdater(locationEngine, listener);
    LocationEngine newLocationEngine = mock(LocationEngine.class);

    provider.updateLocationEngine(newLocationEngine);

    verify(newLocationEngine).addLocationEngineListener(eq(listener));
  }

  @Test
  public void forceLocationUpdate_nonNullLastLocationIsSent() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    when(locationEngine.getLastLocation()).thenReturn(mock(Location.class));
    NavigationLocationEngineListener listener = mock(NavigationLocationEngineListener.class);
    when(listener.isValidLocationUpdate(any(Location.class))).thenReturn(true);
    NavigationLocationEngineUpdater provider = new NavigationLocationEngineUpdater(locationEngine, listener);

    provider.forceLocationUpdate(mock(DirectionsRoute.class));

    verify(listener).queueLocationUpdate(any(Location.class));
  }

  @Test
  public void removeLocationEngineListener() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    NavigationLocationEngineListener listener = mock(NavigationLocationEngineListener.class);
    NavigationLocationEngineUpdater provider = new NavigationLocationEngineUpdater(locationEngine, listener);

    provider.removeLocationEngineListener();

    verify(locationEngine).removeLocationEngineListener(eq(listener));
  }
}