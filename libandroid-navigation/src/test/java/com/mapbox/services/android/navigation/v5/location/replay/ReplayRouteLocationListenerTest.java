package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngineCallback;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ReplayRouteLocationListenerTest {

  @Test
  public void onLocationReplay_lastMockedLocationRemoved() {
    ReplayRouteLocationEngine engine = mock(ReplayRouteLocationEngine.class);
    LocationEngineCallback callback = mock(LocationEngineCallback.class);
    ReplayRouteLocationListener listener = new ReplayRouteLocationListener(engine, callback);

    listener.onLocationReplay(mock(Location.class));

    verify(engine).removeLastMockedLocation();
  }

  @Test
  public void onLocationReplay_updateLastLocation() {
    ReplayRouteLocationEngine engine = mock(ReplayRouteLocationEngine.class);
    LocationEngineCallback callback = mock(LocationEngineCallback.class);
    Location location = mock(Location.class);
    ReplayRouteLocationListener listener = new ReplayRouteLocationListener(engine, callback);

    listener.onLocationReplay(location);

    verify(engine).updateLastLocation(location);
  }
}