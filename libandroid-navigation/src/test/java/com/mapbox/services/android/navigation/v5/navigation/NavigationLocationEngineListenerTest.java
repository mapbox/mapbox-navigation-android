package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.services.android.navigation.v5.location.LocationValidator;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationLocationEngineListenerTest {

  @Test
  public void onConnected_engineRequestsUpdates() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    NavigationLocationEngineListener listener = buildListener(locationEngine);

    listener.onConnected();

    verify(locationEngine).requestLocationUpdates();
  }

  @Test
  public void queueValidLocationUpdate_threadReceivesUpdate() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    NavigationLocationEngineListener listener = buildListener(thread);
    Location location = mock(Location.class);

    listener.onLocationChanged(location);

    verify(thread).updateRawLocation(location);
  }

  private NavigationLocationEngineListener buildListener(RouteProcessorBackgroundThread thread) {
    return new NavigationLocationEngineListener(thread, mock(LocationEngine.class),
      mock(LocationValidator.class));
  }

  private NavigationLocationEngineListener buildListener(LocationEngine locationEngine) {
    return new NavigationLocationEngineListener(mock(RouteProcessorBackgroundThread.class),
      locationEngine, mock(LocationValidator.class));
  }
}