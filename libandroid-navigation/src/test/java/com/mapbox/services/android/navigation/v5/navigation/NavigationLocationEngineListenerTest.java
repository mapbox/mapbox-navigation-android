package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class NavigationLocationEngineListenerTest {

  @Test
  public void onConnected_engineRequestsUpdates() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    NavigationLocationEngineListener listener = buildListener(locationEngine);

    listener.onConnected();

    verify(locationEngine).requestLocationUpdates();
  }

  @Test
  public void onConnected_nonNullLastLocationIsSent() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    Location location = mock(Location.class);
    when(locationEngine.getLastLocation()).thenReturn(location);
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    NavigationLocationEngineListener listener = new NavigationLocationEngineListener(
      thread, locationEngine
    );

    listener.onConnected();

    verify(thread).updateRawLocation(location);
  }

  @Test
  public void onConnected_nullLastLocationIsIgnored() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    when(locationEngine.getLastLocation()).thenReturn(null);
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    NavigationLocationEngineListener listener = new NavigationLocationEngineListener(
      thread, locationEngine
    );

    listener.onConnected();

    verifyZeroInteractions(thread);
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
    return new NavigationLocationEngineListener(thread, mock(LocationEngine.class));
  }

  private NavigationLocationEngineListener buildListener(LocationEngine locationEngine) {
    return new NavigationLocationEngineListener(mock(RouteProcessorBackgroundThread.class), locationEngine);
  }
}