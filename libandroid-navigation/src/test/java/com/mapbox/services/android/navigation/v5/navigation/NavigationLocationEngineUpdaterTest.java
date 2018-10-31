package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.android.core.location.LocationEngine;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
  public void updateLocationEngine_oldEngineListenerIsRemoved() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    NavigationLocationEngineListener listener = mock(NavigationLocationEngineListener.class);
    NavigationLocationEngineUpdater provider = new NavigationLocationEngineUpdater(locationEngine, listener);
    LocationEngine newLocationEngine = mock(LocationEngine.class);

    provider.updateLocationEngine(newLocationEngine);

    verify(locationEngine).removeLocationEngineListener(eq(listener));
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